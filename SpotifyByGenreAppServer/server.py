import requests
from flask import Flask, request, jsonify

app = Flask(__name__)

def get_all_liked_tracks(access_token):
    headers = {'Authorization': f'Bearer {access_token}'}
    all_tracks = []
    limit = 50
    offset = 0
    while True:
        url = f"https://api.spotify.com/v1/me/tracks?limit={limit}&offset={offset}"
        res = requests.get(url, headers=headers)
        if res.status_code != 200:
            return None, f"Error obteniendo canciones: {res.status_code}, {res.text}"
        data = res.json()
        items = data.get("items", [])
        all_tracks.extend(items)
        if len(items) < limit:
            break
        offset += limit
    return all_tracks, None

def get_artists_genres(access_token, artist_ids):
    headers = {'Authorization': f'Bearer {access_token}'}
    artist_genres_map = {}
    artist_ids = list(artist_ids)
    for i in range(0, len(artist_ids), 50):
        batch = artist_ids[i:i+50]
        url = "https://api.spotify.com/v1/artists?ids=" + ",".join(batch)
        res = requests.get(url, headers=headers)
        if res.status_code != 200:
            return None, f"Error obteniendo géneros: {res.status_code}, {res.text}"
        data = res.json()
        for artist in data.get("artists", []):
            artist_genres_map[artist["id"]] = artist["genres"]
    return artist_genres_map, None

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

@app.route("/liked_songs_genres")
def liked_songs_genres():
    access_token = request.args.get("access_token")
    if not access_token:
        return "No access token received.", 400

    # 1. Obtener todas las canciones "liked" (paginación)
    all_tracks, error = get_all_liked_tracks(access_token)
    if error:
        return error, 400

    liked_tracks = []
    all_artist_ids = set()

    # 2. Extraer IDs de TODOS los artistas de cada pista
    for item in all_tracks:
        track_info = item["track"]
        artist_ids = [artist["id"] for artist in track_info["artists"]]
        all_artist_ids.update(artist_ids)
        liked_tracks.append({
            "track_name": track_info["name"],
            "artist_names": [artist["name"] for artist in track_info["artists"]],
            "artist_ids": artist_ids,
            "genres": []  # Se completará con la unión de géneros de todos los artistas
        })

    # 3. Obtener géneros de todos los artistas (en lotes)
    artist_genres_map, error = get_artists_genres(access_token, all_artist_ids)
    if error:
        return error, 400

    # 4. Combinar géneros para cada pista
    for track in liked_tracks:
        combined_genres = set()
        for aid in track["artist_ids"]:
            combined_genres.update(artist_genres_map.get(aid, []))
        track["genres"] = list(combined_genres)

    # 5. Clasificar las canciones en categorías amplias
    broad_categories = {
        "Rock": ["rock", "mexican rock", "latin rock", "rock urbano", "rock en español"],
        "Corridos": ["corrido", "corridos tumbados", "corridos bélicos"],
        "Punk": ["punk", "ska punk", "hardcore punk", "skate punk"],
        "Hip Hop": ["cloud rap", "emo rap", "underground hip hop", "dark trap", "punk rap"],
        "Indie": ["indie", "mexican indie", "latin indie"],
        "Latin": ["latin alternative", "latin folk", "bolero", "mariachi", "colombian pop", "reggaeton mexa", "norteño"]
    }

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

    return jsonify(songs_by_category)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8888, debug=True)
