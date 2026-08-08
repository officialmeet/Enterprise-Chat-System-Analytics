# 💬 Enterprise Multi-Threaded Chat Hub & Analytics

An enterprise-grade Java application built with multi-threaded Socket programming to manage real-time user connections, custom channels (`/join`), and private direct messages (`/msg`)[cite: 3]. Interaction transcripts are logged to `chat_logs.csv` for Excel reporting, SQL analysis, and Python NLP sentiment analytics[cite: 3].

---

## 🌟 Key Features
- **Multi-Threaded Socket Routing:** Real-time client handling using `ConcurrentHashMap`[cite: 3].
- **Channel & DM System:** Switch channels (`/join datascience`) or send private messages (`/msg user hello`)[cite: 3].
- **Automated CSV Persistence:** Logs timestamps, channels, senders, recipients, and messages to `chat_logs.csv`[cite: 3].
- **Data Science Integration:** Python NLP sentiment analysis script (`chat_analysis.py`) and SQL queries[cite: 3].

---

## 🛠️ Tech Stack
- **Backend:** Java 17+ (Sockets, Concurrency)
- **Data Science:** Python 3.10+ (Pandas, NLTK Sentiment Intensity Analyzer)
- **Database / SQL:** DuckDB / MySQL
