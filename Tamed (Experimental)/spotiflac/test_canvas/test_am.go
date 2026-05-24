package main

import (
	"fmt"
	"io"
	"net/http"
	"net/url"
)

func main() {
	query := url.QueryEscape("Solitude Is Bliss Tame Impala")
	req, _ := http.NewRequest("GET", "https://lyrics.paxsenix.org/apple-music/search?q="+query, nil)
	req.Header.Set("User-Agent", "SpotiFlac/1.0")
	resp, _ := http.DefaultClient.Do(req)
	body, _ := io.ReadAll(resp.Body)
	fmt.Println(string(body))
}
