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
            {'title': '12th Edition of UMA Symposium', 'subtitle': 'Nature/Culture', 'desc': 'Annual symposium exploring nature and culture.', 'date': 'Nov 12-14, 2025', 'img': 'symposium', 'order': 1},
            {'title': 'UMA Culture Day 25', 'subtitle': 'Carthage El Hadatha', 'desc': 'Celebrating cultural heritage and diversity.', 'date': 'Dec 10-11, 2025', 'img': 'cultureday', 'order': 2},
            {'title': 'TuniHack 11.0', 'subtitle': 'National Hackathon', 'desc': 'The biggest student hackathon in Tunisia returns for its 11th edition.', 'date': 'Feb 20-22, 2026', 'img': 'tunihack_11_0', 'order': 3},
            {'title': 'RoboCup ENSI 8', 'subtitle': 'Robotics Competition', 'desc': 'Eighth edition of the prestigious national robotics challenge.', 'date': 'March 15, 2026', 'img': 'robocup_ensi_8', 'order': 4},
            {'title': 'Hackathon Green UMA', 'subtitle': 'Sustainable Tech', 'desc': 'Focusing on environmental challenges and green solutions.', 'date': 'Jan 31 - Feb 1, 2026', 'img': 'hackaton_uma', 'order': 5},
            {'title': 'Career Fair 3.0', 'subtitle': 'Professional Networking', 'desc': 'Connect with top tech companies and secure your future career.', 'date': 'April 5, 2026', 'img': 'career_fair_3_0', 'order': 6},
            {'title': 'Code Conquer 3.0', 'subtitle': 'Coding Challenge', 'desc': 'A high-intensity competitive programming event.', 'date': 'May 12, 2026', 'img': 'code_conquer_3_0', 'order': 7},
            {'title': 'ESENet Talent Fair 7', 'subtitle': 'Job Fair', 'desc': 'The annual talent meeting at the Higher School of Digital Economy.', 'date': 'May 20, 2026', 'img': 'esenet_talent_fair_7', 'order': 8},
            {'title': 'GeoDrone Day', 'subtitle': 'Geospatial Innovation', 'desc': 'Exploring the use of drones in mapping and geospatial science.', 'date': 'June 2, 2026', 'img': 'geodrone_day', 'order': 9},
            {'title': 'MSE Hack 1.0', 'subtitle': 'Serious Games Hackathon', 'desc': 'Design and build the next generation of educational serious games.', 'date': 'June 18-20, 2026', 'img': 'mse_hack_1_0', 'order': 10},
            {'title': 'Orbyx ML Challenge', 'subtitle': 'Machine Learning', 'desc': 'Deep dive into AI and ML with real-world datasets.', 'date': 'July 5, 2026', 'img': 'orbyx_ml_challenge', 'order': 11},
            {'title': 'Green Fortnight', 'subtitle': 'Environmental Awareness', 'desc': 'Two weeks of activities dedicated to campus ecology.', 'date': 'October 1-15, 2025', 'img': 'green_fortnight', 'order': 12},
            {'title': 'ESEN Brain Games Expo', 'subtitle': 'Gaming & Strategy', 'desc': 'A festival of strategic thinking and digital gaming.', 'date': 'October 25, 2025', 'img': 'esen_brain_games_expo', 'order': 13},
            {'title': 'Recursia 2025', 'subtitle': 'Computing Festival', 'desc': 'Celebrating the art of algorithms and recursion.', 'date': 'December 5, 2025', 'img': 'recursia', 'order': 14},
            {'title': 'Manouba Networking Day', 'subtitle': 'Alumni Meeting', 'desc': 'Annual networking event connecting students with alumni.', 'date': 'April 30, 2025', 'img': 'networkingday', 'order': 15},
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
