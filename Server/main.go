
package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
    "log"
	"net/http"
)

type SMS struct {
	Sender string
	Content string
	Date string
}

var pool map[string][]SMS

func loadData() error {
    pool = make(map[string][]SMS)

    files, _ := ioutil.ReadDir("./data")
    for _, f := range files {
        receiver := f.Name()
        if len(receiver) != 11 || receiver[0] != '1' {
            continue
        }

        raw, err := ioutil.ReadFile("./data/" + receiver)
        if err != nil {
            return err
        }

        var collection []SMS
        err = json.Unmarshal(raw, &collection)
        if err != nil {
            return err
        }
        pool[receiver] = collection
    }

    return nil
}

func saveCollection(receiver string) error {
    raw, err := json.Marshal(pool[receiver])
    if err != nil {
        return err
    }

	fileName := "./data/" + receiver
	return ioutil.WriteFile(fileName, raw, 0600)
}

func viewHandler(w http.ResponseWriter, r *http.Request) {
	receiver := r.URL.Path[len("/view/"):]
    if len(receiver) == 0 {
        if len(pool) > 0 {
            for rcv, _ := range pool {
                fmt.Fprintf(w, "<p><a href='/view/%s'>%s</a></p>\n", rcv, rcv)
            }
        } else {
            fmt.Fprintf(w, "No SMS received yet.")
        }

        return
    }

    collection, existed := pool[receiver]
    if !existed {
        fmt.Fprintf(w, "No SMS received yet for %s.", receiver)
        return
    }

    for i := 0; i < len(collection); i++ {
        fmt.Fprintf(w, "<p>%s</p>\n", collection[i].Content)
    }
}

func postHandler(w http.ResponseWriter, r *http.Request) {
	receiver := r.URL.Path[len("/post/"):]
    if len(receiver) == 0 {
        log.Printf("No receiver provided.")
        return
    }

    decoder := json.NewDecoder(r.Body)
	var sms SMS
    err := decoder.Decode(&sms)
    if err != nil {
        log.Printf("Illege JSON data: %v", err)
		return
    }
    defer r.Body.Close()

    pool[receiver] = append(pool[receiver], sms)
    saveCollection(receiver)

    feedback := fmt.Sprintf("New SMS received from %s to %s\n", sms.Sender, receiver)
    log.Print(feedback)
    fmt.Fprint(w, feedback)
}

func main() {
    if err := loadData(); err != nil {
        log.Fatalf("Cannot load saved data: %v", err)
        return
    }

	port := 1992
	http.HandleFunc("/view/", viewHandler)
	http.HandleFunc("/post/", postHandler)

	fmt.Printf("Serving on port %d...\n", port)
	http.ListenAndServe(fmt.Sprintf(":%d", port), nil)
}

