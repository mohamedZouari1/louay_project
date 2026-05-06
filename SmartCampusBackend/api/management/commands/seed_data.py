from django.core.management.base import BaseCommand
from django.contrib.auth.models import User
from api.models import CampusLocation, Event, CampusStat

class Command(BaseCommand):
    help = 'Seed the database with University of Manouba data'

    def handle(self, *args, **kwargs):
        self.stdout.write('Seeding data...')

        # 1. Campus Locations
        locations = [
            {'name': 'ENSI - National School of Computer Science', 'category': 'Research', 'lat': 36.8138, 'lng': 10.0632},
            {'name': 'ISAMM - Higher Institute of Multimedia Arts', 'category': 'Research', 'lat': 36.8125, 'lng': 10.0650},
            {'name': 'University Library', 'category': 'Libraries', 'lat': 36.8130, 'lng': 10.0640},
            {'name': 'Campus Café', 'category': 'Cafés', 'lat': 36.8140, 'lng': 10.0630},
            {'name': 'Student Services Center', 'category': 'Services', 'lat': 36.8120, 'lng': 10.0620},
        ]

        for loc in locations:
            CampusLocation.objects.get_or_create(
                name=loc['name'],
                category=loc['category'],
                defaults={'latitude': loc['lat'], 'longitude': loc['lng'], 'description': 'Main campus building'}
            )

        # 2. Events
        events = [
            {'title': 'Annual Tech Symposium', 'subtitle': 'The future of GeoAI', 'desc': 'Join us for a full day of talks about ecosystem restoration and AI.', 'date': 'June 15, 2026', 'order': 1},
            {'title': 'Ecosystem Restoration Hackathon', 'subtitle': 'Build for the planet', 'desc': 'A 48-hour challenge to build digital tools for a greener campus.', 'date': 'July 10-12, 2026', 'order': 2},
            {'title': 'Career Fair 2026', 'subtitle': 'Meet your future employers', 'desc': 'Networking event with top technology companies in Tunisia.', 'date': 'September 5, 2026', 'order': 3},
        ]

        for ev in events:
            Event.objects.get_or_create(
                title=ev['title'],
                defaults={'subtitle': ev['subtitle'], 'description': ev['desc'], 'date_display': ev['date'], 'order': ev['order']}
            )

        # 3. Stats
        stats = [
            {'label': 'Active Students', 'value': '15k+', 'icon': 'students', 'order': 1},
            {'label': 'Research Labs', 'value': '24', 'icon': 'labs', 'order': 2},
            {'label': 'Green Zones', 'params': '60%', 'icon': 'nature', 'order': 3},
        ]

        for s in stats:
            CampusStat.objects.get_or_create(
                label=s['label'],
                defaults={'value': s.get('value', s.get('params')), 'icon': s['icon'], 'order': s['order']}
            )

        self.stdout.write(self.style.SUCCESS('Successfully seeded Smart Campus data!'))
