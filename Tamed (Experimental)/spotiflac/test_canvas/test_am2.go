package main
import (
	"fmt"
	"io"
	"net/http"
)
func main() {
    req, _ := http.NewRequest("GET", "https://lyrics.paxsenix.org/apple-music/song?id=1837237747", nil)
    req.Header.Set("User-Agent", "SpotiFlac/1.0")
	resp, _ := http.DefaultClient.Do(req)
	body, _ := io.ReadAll(resp.Body)
	fmt.Println(string(body))
}
