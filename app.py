import os

from env_utils import load_local_env
load_local_env()

from dashboard import app

demo_mode = os.getenv("DEMO_MODE", "false").strip().lower() == "true"
if demo_mode:
    from dashboard import set_agent_status
    set_agent_status("active")

if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    debug = os.getenv("FLASK_DEBUG", "false").strip().lower() == "true"

    if demo_mode:
        print(f"[DEMO] Demo mode active. Open http://localhost:{port}")
    else:
        import threading
        import crash_engine
        from notifier import start_bot_polling

        t = threading.Thread(target=crash_engine.run_crash_engine, daemon=True)
        t.start()
        start_bot_polling()

    app.run(host="0.0.0.0", port=port, debug=debug)
