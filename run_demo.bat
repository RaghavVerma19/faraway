@echo off
echo ========================================
echo   NewsPulse Crash Tracker - Demo Mode
echo ========================================
echo.
echo Starting server with demo data...
echo.

set DEMO_MODE=true
python seed_demo.py
python app.py
