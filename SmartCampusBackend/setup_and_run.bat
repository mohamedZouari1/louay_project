@echo off
echo ==========================================
echo   Smart Campus Backend Setup
echo ==========================================
echo.

echo [1/4] Creating virtual environment...
python -m venv venv
call venv\Scripts\activate

echo [2/4] Installing dependencies...
pip install -r requirements.txt

echo [3/4] Running migrations...
python manage.py makemigrations api
python manage.py migrate

echo [4/4] Loading campus data...
python manage.py load_campus_data

echo.
echo ==========================================
echo   Setup complete! Starting server...
echo ==========================================
echo.
echo   Backend: http://127.0.0.1:8000/api/
echo   Admin:   http://127.0.0.1:8000/admin/
echo.
python manage.py runserver 0.0.0.0:8000
