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
            {
                'title': '12th Edition of UMA Symposium',
                'subtitle': 'Nature/Culture',
                'desc': 'Annual symposium exploring the intersection of nature and culture.',
                'date': 'Nov 12-14, 2025',
                'img': 'symposium',
                'order': 1
            },
            {
                'title': 'UMA Culture Day 25',
                'subtitle': 'Carthage El Hadatha',
                'desc': "Celebrate the rich cultural heritage of the University of Manouba community.",
                'date': 'Dec 10-11, 2025',
                'img': 'cultureday',
                'order': 2
            },
            {
                'title': 'Hackathon Green UMA',
                'subtitle': 'CIFIPP Lac 2',
                'desc': 'Innovation hackathon focused on sustainable technology solutions.',
                'date': 'Jan 31 - Feb 1, 2026',
                'img': 'hackaton_uma',
                'order': 3
            },
            {
                'title': 'Manouba Networking Day',
                'subtitle': 'Campus universitaire',
                'desc': 'Annual networking event connecting students with industry professionals.',
                'date': 'Apr 30, 2025',
                'img': 'networkingday',
                'order': 4
            },
        ]

        for ev in events:
            Event.objects.get_or_create(
                title=ev['title'],
                defaults={
                    'subtitle': ev['subtitle'],
                    'description': ev['desc'],
                    'date_display': ev['date'],
                    'image_name': ev['img'],
                    'order': ev['order']
                }
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
