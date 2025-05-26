# -------------------------------
# IMPORTACIONES
# -------------------------------
import requests  # Para hacer peticiones HTTP a la API de Spotify
from flask import Flask, request, jsonify  # Flask básico para web API
from flask_cors import CORS  # Permite llamadas desde apps externas (como Android)

# -------------------------------
# CONFIGURACIÓN FLASK
# -------------------------------
app = Flask(__name__)
CORS(app)  # Habilita CORS en toda la app para evitar bloqueos por política de origen cruzado

# -------------------------------
# 1. FUNCIÓN: Obtener canciones "liked" del usuario
# -------------------------------
def get_all_liked_tracks(access_token):
    headers = {'Authorization': f'Bearer {access_token}'}  # Autorización para llamar a Spotify
    all_tracks = []  # Aquí se irán guardando todas las canciones
    limit = 50  # Spotify permite hasta 50 canciones por llamada
    offset = 0  # Para paginar

    while True:
        url = f"https://api.spotify.com/v1/me/tracks?limit={limit}&offset={offset}"
        res = requests.get(url, headers=headers)

        if res.status_code != 200:
            return None, f"Error obteniendo canciones: {res.status_code}, {res.text}"

        data = res.json()
        items = data.get("items", [])
        all_tracks.extend(items)  # Agrega las canciones actuales al total

        if len(items) < limit:
            break  # Ya no hay más canciones, sal del ciclo

        offset += limit  # Pasa a la siguiente página

    return all_tracks, None

# -------------------------------
# 2. FUNCIÓN: Obtener géneros de todos los artistas
# -------------------------------
def get_artists_genres(access_token, artist_ids):
    headers = {'Authorization': f'Bearer {access_token}'}
    artist_genres_map = {}
    artist_ids = list(artist_ids)  # Convertir el set en lista

    # Spotify permite consultar hasta 50 artistas por llamada
    for i in range(0, len(artist_ids), 50):
        batch = artist_ids[i:i+50]
        url = "https://api.spotify.com/v1/artists?ids=" + ",".join(batch)
        res = requests.get(url, headers=headers)

        if res.status_code != 200:
            return None, f"Error obteniendo géneros: {res.status_code}, {res.text}"

        data = res.json()
        for artist in data.get("artists", []):
            artist_genres_map[artist["id"]] = artist.get("genres", [])  # Si no hay géneros, se pone lista vacía

    return artist_genres_map, None

# -------------------------------
# 3. RUTA DE PRUEBA: Solo para navegador (callback manual)
# -------------------------------
@app.route("/callback")
def callback():
    return """
    <script>
        const fragment = new URLSearchParams(window.location.hash.substring(1));
        const accessToken = fragment.get("access_token");
        if (accessToken) {
            window.location.href = "/liked_songs_genres?access_token=" + accessToken;
        } else {
            document.body.innerHTML = "<h2>No access token received.</h2>";
        }
    </script>
    """

# -------------------------------
# 4. RUTA PRINCIPAL: Clasificar y devolver canciones por género
# -------------------------------
@app.route("/liked_songs_genres")
def liked_songs_genres():
    access_token = request.args.get("access_token")
    if not access_token:
        return "No access token received.", 400

    # 4.1 Obtener todas las canciones "liked" del usuario
    all_tracks, error = get_all_liked_tracks(access_token)
    if error:
        return error, 400

    liked_tracks = []
    all_artist_ids = set()

    # 4.2 Extraer info clave de cada track y recolectar IDs de artistas
    for item in all_tracks:
        track_info = item.get("track")
        if not track_info or "id" not in track_info or "name" not in track_info or "artists" not in track_info:
            continue  # Evita errores si falta información

        artist_ids = [artist["id"] for artist in track_info["artists"]]
        all_artist_ids.update(artist_ids)

        liked_tracks.append({
            "track_id": track_info["id"],
            "track_name": track_info["name"],
            "artist_names": [artist["name"] for artist in track_info["artists"]],
            "artist_ids": artist_ids,
            "genres": []  # Se llenará más adelante
        })

    # 4.3 Obtener géneros por cada artista
    artist_genres_map, error = get_artists_genres(access_token, all_artist_ids)
    if error:
        return error, 400

    # 4.4 Asignar géneros combinados a cada canción
    for track in liked_tracks:
        combined_genres = set()
        for aid in track["artist_ids"]:
            combined_genres.update(artist_genres_map.get(aid, []))
        track["genres"] = list(combined_genres)

    # 4.5 Definir categorías personalizadas por familia de género
    broad_categories = {
        "Rock": ["rock", "mexican rock", "latin rock", "rock urbano", "rock en español"],
        "Corridos": ["corrido", "corridos tumbados", "corridos bélicos"],
        "Punk": ["punk", "ska punk", "hardcore punk", "skate punk"],
        "Hip Hop": ["cloud rap", "emo rap", "underground hip hop", "dark trap", "punk rap"],
        "Indie": ["indie", "mexican indie", "latin indie"],
        "Latin": ["latin alternative", "latin folk", "bolero", "mariachi", "colombian pop", "reggaeton mexa", "norteño"]
    }

    # 4.6 Clasificar canciones en cada categoría según sus géneros
    songs_by_category = {}

    for track in liked_tracks:
        if not track["genres"]:
            songs_by_category.setdefault("Desconocido", []).append(track)
        else:
            assigned = False
            for g in track["genres"]:
                for category, keywords in broad_categories.items():
                    if any(kw.lower() in g.lower() for kw in keywords):
                        songs_by_category.setdefault(category, []).append(track)
                        assigned = True
            if not assigned:
                songs_by_category.setdefault("Otros", []).append(track)

    # 4.7 Devolver solo las categorías y canciones clasificadas
    return jsonify(songs_by_category)

# -------------------------------
# 5. INICIO DEL SERVIDOR
# -------------------------------
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8888, debug=True)
