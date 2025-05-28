# -------------------------------
# IMPORTACIONES
# -------------------------------
import requests  # Para hacer peticiones HTTP a Spotify
from flask import Flask, request, jsonify  # Para crear el servidor web y manejar peticiones
from flask_cors import CORS  # Para permitir acceso desde Android o cualquier frontend externo

# -------------------------------
# CONFIGURACIÓN FLASK
# -------------------------------
app = Flask(__name__)
print("✅ server.py CARGADO")
CORS(app)  # Habilita CORS para evitar bloqueos por políticas de origen cruzado

# -------------------------------
# 1. FUNCIÓN: Obtener canciones "liked" del usuario
# -------------------------------
def get_all_liked_tracks(access_token):
    headers = {'Authorization': f'Bearer {access_token}'}
    all_tracks = []
    limit = 50
    offset = 0
    total_expected = None

    # Paginación para obtener todas las canciones liked del usuario
    while True:
        url = f"https://api.spotify.com/v1/me/tracks?limit={limit}&offset={offset}"
        res = requests.get(url, headers=headers)

        if res.status_code != 200:
            print("❌ Error al obtener canciones:", res.text)
            return [], "Error al llamar a Spotify"

        data = res.json()

        if total_expected is None:
            total_expected = data.get("total", 0)

        items = data.get("items", [])
        if not items:
            break

        all_tracks.extend(items)

        if len(items) < limit:
            break

        offset += limit

    # ✅ Validar solo canciones que realmente tienen ID, nombre y objeto track
    valid_tracks = [
        item for item in all_tracks
        if item.get("track") and item["track"].get("id") and item["track"].get("name")
    ]

    print(f"🎧 Total esperado (Spotify): {total_expected}")
    print(f"📦 Total recibido (raw): {len(all_tracks)}")
    print(f"🎯 Canciones válidas detectadas: {len(valid_tracks)}")

    return valid_tracks, len(valid_tracks)


# -------------------------------
# 2. FUNCIÓN: Obtener géneros de todos los artistas
# -------------------------------
def get_artists_genres(access_token, artist_ids):
    headers = {'Authorization': f'Bearer {access_token}'}
    artist_genres_map = {}
    artist_ids = list(artist_ids)

    # Spotify permite hasta 50 artistas por petición
    for i in range(0, len(artist_ids), 50):
        batch = artist_ids[i:i+50]
        url = "https://api.spotify.com/v1/artists?ids=" + ",".join(batch)
        res = requests.get(url, headers=headers)

        if res.status_code != 200:
            return None, f"Error obteniendo géneros: {res.status_code}, {res.text}"

        data = res.json()
        for artist in data.get("artists", []):
            artist_genres_map[artist["id"]] = artist.get("genres", [])

    return artist_genres_map, None

# -------------------------------
# 3. RUTA DE PRUEBA: Solo para navegador (no app)
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
# 4. RUTA PRINCIPAL: Clasificación por género
# -------------------------------
@app.route("/liked_songs_genres")
def liked_songs_genres():
    access_token = request.args.get("access_token")
    if not access_token:
        return "No access token received.", 400

    # Obtener canciones liked y cantidad total esperada
    all_tracks, total_expected = get_all_liked_tracks(access_token)

    liked_tracks = []
    all_artist_ids = set()

    # Extraer información esencial por track
    for item in all_tracks:
        track_info = item.get("track")
        if not track_info or "id" not in track_info or "name" not in track_info or "artists" not in track_info:
            continue

        artist_ids = [artist["id"] for artist in track_info["artists"]]
        all_artist_ids.update(artist_ids)

        liked_tracks.append({
            "track_id": track_info["id"],
            "track_name": track_info["name"],
            "artist_names": [artist["name"] for artist in track_info["artists"]],
            "artist_ids": artist_ids,
            "genres": []
        })

    # Obtener géneros de todos los artistas del usuario
    artist_genres_map, error = get_artists_genres(access_token, all_artist_ids)
    if error:
        return error, 400

    # Asignar géneros combinados a cada canción según sus artistas
    for track in liked_tracks:
        combined_genres = set()
        for aid in track["artist_ids"]:
            combined_genres.update(artist_genres_map.get(aid, []))
        track["genres"] = list(combined_genres)

    # Lista de categorías amplias para clasificar
    broad_categories = {
        "Rock": [
            "rock", "garage", "latin rock", "rock urbano", "rock en español",
            "hard rock", "classic rock", "psychedelic rock", "grunge"
        ],
        "Pop": [
            "pop", "pop latino", "latin pop", "synthpop", "pop rock",
            "teen pop", "indie pop", "electropop"
        ],
        "Punk": [
            "punk", "ska punk", "hardcore punk", "skate punk", "punk rock",
            "melodic hardcore", "punk rap"
        ],
        "Hip Hop / Rap": [
            "rap", "hip hop", "trap", "latin hip hop", "cloud rap",
            "emo rap", "boom bap", "underground hip hop", "drill"
        ],
        "Indie / Alternativo": [
            "indie", "indie rock", "indie folk", "mexican indie", "latin indie",
            "alternative", "alt-rock", "lo-fi", "bedroom pop"
        ],
        "Metal": [
            "metal", "death metal", "metalcore", "thrash metal", "heavy metal",
            "black metal", "doom metal"
        ],
        "Electronic": [
            "edm", "electronic", "house", "techno", "dubstep", "synthwave",
            "electro", "future bass", "trance", "drum and bass"
        ],
        "R&B / Soul": [
            "r&b", "soul", "neo soul", "funk", "contemporary r&b",
            "motown", "quiet storm"
        ],
        "Reggaetón / Urbano": [
            "reggaeton", "latin trap", "urbano latino", "dembow",
            "trap latino", "reggaeton mexa"
        ],
        "Corridos / Regional Mexicano": [
            "corrido", "corridos tumbados", "corridos bélicos",
            "norteño", "mariachi", "banda", "grupera"
        ],
        "Folk / World / Tradicional": [
            "bolero", "folk", "latin folk", "flamenco", "música andina",
            "cumbia", "vallenato", "chicha", "world"
        ],
        "Otros / Desconocido": []  # Para lo que no encaje en ninguna categoría
    }

    songs_by_category = {}

    # Clasificar canciones en todas las categorías relevantes
    for track in liked_tracks:
        track_id = track["track_id"]

        if not track["genres"]:
            existing_ids = [t["track_id"] for t in songs_by_category.get("Otros / Desconocido", [])]
            if track_id not in existing_ids:
                songs_by_category.setdefault("Otros / Desconocido", []).append(track)
            continue

        assigned = False
        for g in track["genres"]:
            for category, keywords in broad_categories.items():
                if any(kw.lower() in g.lower() for kw in keywords):
                    existing_ids = [t["track_id"] for t in songs_by_category.get(category, [])]
                    if track_id not in existing_ids:
                        songs_by_category.setdefault(category, []).append(track)
                    assigned = True  # Nota: no usamos break → clasificación múltiple

        if not assigned:
            existing_ids = [t["track_id"] for t in songs_by_category.get("Otros / Desconocido", [])]
            if track_id not in existing_ids:
                songs_by_category.setdefault("Otros / Desconocido", []).append(track)

    # Respuesta JSON para Android
    return jsonify({
        "genres": songs_by_category,
        "total_expected": total_expected,
        "total_received": len(all_tracks)
    })

# -------------------------------
# 5. INICIO DEL SERVIDOR
# -------------------------------
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8888, debug=True)
