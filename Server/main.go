package main

import (
	"encoding/json"
	"fmt"
	"html/template"
	"io/ioutil"
	"log"
	"net/http"
)

type SMS struct {
	ID      int
	Sender  string
	Content string
	Date    string
}

var pool map[string][]SMS
var ids map[int]bool

func sub(x, y int) int {
	return x - y
}

var numbersTmpl = template.Must(template.New("view_number_list.html").
	ParseFiles("./tmpl/view_number_list.html"))

var viewTmpl = template.Must(template.New("view.html").
	Funcs(template.FuncMap{"sub": sub}).
	ParseFiles("./tmpl/view.html"))

func loadData() error {
	pool = make(map[string][]SMS)
	ids = make(map[int]bool)

	files, _ := ioutil.ReadDir("./data")
	for _, f := range files {
		if f.Name()[0] == '.' {
			continue
		}

		receiver := f.Name()

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

		for i := 0; i < len(collection); i++ {
			ids[collection[i].ID] = true
		}
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
		numbersTmpl.Execute(w, pool)
		return
	}

	if _, existed := pool[receiver]; !existed {
		fmt.Fprintf(w, "No SMS received yet for %s.", receiver)
		return
	}

	page := struct {
		Title      string
		Collection []SMS
	}{receiver, pool[receiver]}

	err := viewTmpl.Execute(w, page)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
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

	feedback := fmt.Sprintf("SMS received from %s to %s\n", sms.Sender, receiver)

	if _, existed := ids[sms.ID]; !existed {
		pool[receiver] = append(pool[receiver], sms)
		saveCollection(receiver)

		ids[sms.ID] = true

		feedback = "New " + feedback
	} else {
		feedback = "Duplicated " + feedback
	}

	fmt.Fprintf(w, "%d\n%s", sms.ID, feedback)

	log.Print(feedback)
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
