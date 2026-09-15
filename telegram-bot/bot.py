import os
import time
import requests

BOT_TOKEN = os.environ["BOT_TOKEN"]
GITHUB_REPO = "Test-px/PixelMusic"
API = f"https://api.telegram.org/bot{BOT_TOKEN}"

def get_latest_apk():
    url = f"https://api.github.com/repos/{GITHUB_REPO}/releases/latest"
    try:
        data = requests.get(url, timeout=10).json()
    except Exception:
        return None, None
    for asset in data.get("assets", []):
        if asset["name"].endswith(".apk"):
            return asset["browser_download_url"], data.get("tag_name", "latest")
    return None, None

def send_message(chat_id, text):
    requests.post(f"{API}/sendMessage", json={"chat_id": chat_id, "text": text}, timeout=15)

def send_document(chat_id, doc_url, caption):
    requests.post(f"{API}/sendDocument",
                  json={"chat_id": chat_id, "document": doc_url, "caption": caption},
                  timeout=60)

def main():
    offset = 0
    print("Bot started, waiting for messages...")
    while True:
        try:
            r = requests.get(f"{API}/getUpdates",
                             params={"offset": offset, "timeout": 30},
                             timeout=40).json()
        except Exception as e:
            print("Polling error:", e)
            time.sleep(5)
            continue

        for update in r.get("result", []):
            offset = update["update_id"] + 1
            msg = update.get("message")
            if not msg:
                continue
            chat_id = msg["chat"]["id"]
            text = (msg.get("text") or "").strip().lower()

            if text in ("/start", "/apk", "/download"):
                send_message(chat_id, "⏳ Fetching the latest test build...")
                apk_url, tag = get_latest_apk()
                if apk_url:
                    send_document(chat_id, apk_url, f"🚀 Latest Test Build: {tag}")
                else:
                    send_message(chat_id, "❌ No builds available yet. Try again later.")

if __name__ == "__main__":
    main()
