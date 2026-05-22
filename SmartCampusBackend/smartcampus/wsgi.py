import os
from django.core.wsgi import get_wsgi_application

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'smartcampus.settings')
application = get_wsgi_application()

if os.environ.get('RUN_MIGRATIONS_ON_STARTUP', 'True') == 'True':
    from django.core.management import call_command

    call_command('migrate', interactive=False, verbosity=1)
