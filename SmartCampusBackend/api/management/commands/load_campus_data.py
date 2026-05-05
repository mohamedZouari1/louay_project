from django.core.management.base import BaseCommand
from api.models import CampusLocation, Event, CampusStat


class Command(BaseCommand):
    help = 'Load initial campus data (locations, events, stats)'

    def handle(self, *args, **options):
        self.load_stats()
        self.load_events()
        self.load_locations()
        self.stdout.write(self.style.SUCCESS('Successfully loaded all campus data!'))

    def load_stats(self):
        CampusStat.objects.all().delete()
        stats = [
            {'label': 'Students', 'value': '18,601', 'icon': 'people', 'order': 1},
            {'label': 'Teachers', 'value': '1,354', 'icon': 'school', 'order': 2},
            {'label': 'Degree Programs', 'value': '147', 'icon': 'book', 'order': 3},
            {'label': 'Institutions', 'value': '16+', 'icon': 'business', 'order': 4},
            {'label': 'International Students', 'value': '296', 'icon': 'globe', 'order': 5},
            {'label': 'Research Structures', 'value': '31', 'icon': 'flask', 'order': 6},
        ]
        for s in stats:
            CampusStat.objects.create(**s)
        self.stdout.write(f'  Loaded {len(stats)} stats')

    def load_events(self):
        Event.objects.all().delete()
        events = [
            {
                'title': '12th Edition of UMA Symposium',
                'subtitle': 'Caethage El Hadatha',
                'description': 'Annual symposium exploring the intersection of nature and culture in contemporary society.',
                'date_display': 'Nov 12-14, 2025',
                'location': 'Carthage El Hadatha',
                'image_name': 'symposium.gif',
                'order': 1,
            },
            {
                'title': 'UMA Culture Day 25',
                'subtitle': 'Carthage El Hadatha',
                'description': 'Celebrate the rich cultural heritage and diversity of the University of Manouba community.',
                'date_display': 'Dec 10-11, 2025',
                'image_name': 'cultureday.jpg',
                'order': 2,
            },
            {
                'title': '6th Edition of Manouba Networking Day (MND\'25)',
                'subtitle': 'ManoubaCampus',
                'description': 'Annual networking event connecting students with industry professionals and alumni.',
                'date_display': 'Apr 30, 2025',
                'location': 'Campus universitaire de la Manouba',
                'image_name': 'networkingday.jpg',
                'order': 4,
            },
            {
                'title': 'RoboCup ENSI 8.0',
                'subtitle': 'Ecole Nationale des sciences de l\'Informatique',
                'description': 'The 8th edition brings together robotics enthusiasts and technology lovers from all over Tunisia under the theme "Glitched Universe: Chasing Reality." Duration: 12 h.',
                'date_display': 'Oct 19, 2025',
                'location': 'Ecole Nationale des sciences de l\'Informatique',
                'image_name': 'robocup_ensi_8.jpg',
                'order': 5,
            },
            {
                'title': 'Code & Conquer ENSI 3.0',
                'subtitle': 'Ecole Nationale des Sciences de l\'Informatique',
                'description': 'Code & Conquer 3.0 brings a new wave of challenges, creativity, and competitive spirit. This Japanese-themed coding competition challenges you to slice through complex problems, level up your skills, and compete with honor.',
                'date_display': 'Apr 19, 2026',
                'location': 'Ecole Nationale des Sciences de l\'Informatique',
                'image_name': 'code_conquer_3_0.jpg',
                'order': 7,
            },
            {
                'title': 'TuniHack 11.0',
                'subtitle': 'Ecole Nationale des Sciences de l\'Informatique',
                'description': 'This national event brings together brilliant minds to brainstorm, innovate, and build fully functional software solutions in a fast-paced environment.',
                'date_display': 'Jan 24, 2026',
                'location': 'Ecole Nationale des Sciences de l\'Informatique',
                'image_name': 'tunihack_11_0.jpg',
                'order': 8,
            },
            {
                'title': 'Manouba GeoDrone Day - A Hands-On Drone Workshop',
                'subtitle': 'Carthage El Hadatha',
                'description': 'AGEOS co-organizes this immersive GeoDrone Day alongside FLAHM and MSE, bringing together students, researchers, and innovators at the intersection of drones, geomatics, and geospatial intelligence. Time: 09:00-13:00.',
                'date_display': 'Feb 11, 2026',
                'location': 'Amphitheater Carthage Al Hadetha, University of Manouba',
                'image_name': 'geodrone_day.jpg',
                'order': 9,
            },
            {
                'title': 'ESEN Brain Games Expo',
                'subtitle': 'ESEN - Ecole Superieure d\'economie Numerique',
                'description': 'ESEN Brain Games Expo brings together students and thinkers to compete through chess, competitive programming, and critical thinking.',
                'date_display': 'Feb 8, 2026',
                'location': 'La Manouba, Tunisia',
                'image_name': 'esen_brain_games_expo.jpg',
                'order': 10,
            },
            {
                'title': 'Recursia',
                'subtitle': 'ISAMM - Institut Superieur des Arts Multimedia de la Manouba',
                'description': 'Campus ISAMM x ISAMM Problem Solving: learn how to think, analyze, and solve problems like a competitive programmer through interactive sessions leading up to CPC Day at ISAMM.',
                'date_display': 'Feb 14, 2026',
                'location': 'Institut Superieur des Arts Multimedia de la Manouba',
                'image_name': 'recursia.jpg',
                'order': 11,
            },
            {
                'title': 'ESENet Talent Fair - 7th Edition',
                'subtitle': 'ESEN - Ecole Superieure d\'economie Numerique',
                'description': 'ESEN announces the 7th edition of the ESENet Talent Fair under the theme "Synapse: AI to Business," highlighting the intersection of artificial intelligence and business for sustainable value creation.',
                'date_display': 'Nov 26, 2025',
                'location': 'Technopole de La Manouba',
                'image_name': 'esenet_talent_fair_7.jpg',
                'order': 12,
            },
            {
                'title': 'Green Fortnight',
                'subtitle': 'Manouba University',
                'description': 'Be part of a transformative sustainability journey. Participate in curated events designed to inspire environmental action and drive meaningful change. Engage with industry leaders, explore innovative solutions for a greener future, and earn certification.',
                'date_display': 'Apr 13-25, 2026',
                'location': 'Manouba Campus',
                'image_name': 'green_fortnight.jpg',
                'order': 13,
            },
            {
                'title': 'The ORBYX ML Challenge',
                'subtitle': 'Ecole Nationale des Sciences de l\'Informatique',
                'description': 'First edition of one of ENSI\'s most thrilling new events, where data meets innovation and the smartest minds rise to the top.',
                'date_display': 'Nov 22, 2025',
                'location': 'Ecole Nationale des Sciences de l\'Informatique',
                'image_name': 'orbyx_ml_challenge.jpg',
                'order': 14,
            },
        ]
        for e in events:
            Event.objects.create(**e)
        self.stdout.write(f'  Loaded {len(events)} events')

    def load_locations(self):
        CampusLocation.objects.all().delete()
        locations = [
            {"name": "La Poste Tunisienne", "category": "Administration", "description": "Tunisian post office offering mail services, money transfers, and banking.", "hours": "Monday-Friday: 08:00-12:00, 14:30-17:00", "latitude": 36.81304752001674, "longitude": 10.064443131314361, "address": "University Campus Road, near the main entrance"},
            {"name": "Centre de Calcul EL KHAWARIZMI", "category": "Administration", "description": "University computing center providing IT services, network access, and technical support.", "hours": "Monday-Thursday: 08:30-12:30, 13:30-17:30", "phone": "71241900", "website": "https://cck.rnu.tn/", "latitude": 36.81376903057681, "longitude": 10.063144942147385, "address": "University Campus Center, next to ENSI"},
            {"name": "Manouba Technopole Administrative Department", "category": "Administration", "description": "Administrative offices for Manouba's technology park.", "phone": "22472896", "latitude": 36.805964677255226, "longitude": 10.072786753975695},
            {"name": "APII Manouba", "category": "Administration", "description": "Agency for Promotion of Industry and Innovation.", "phone": "70526364", "latitude": 36.806422956913615, "longitude": 10.07187974114076},
            {"name": "Centre de Publication Universitaire", "category": "Administration", "description": "University publishing center handling academic publications.", "latitude": 36.815057974965825, "longitude": 10.064523049672694},
            {"name": "Café l'aigle d'or", "category": "Cafés", "description": "Popular café spot for students to relax and study.", "latitude": 36.815299969381556, "longitude": 10.057800307949849},
            {"name": "Café", "category": "Cafés", "description": "Local café serving coffee, tea, and light snacks.", "latitude": 36.80935292458115, "longitude": 10.075208715894316},
            {"name": "Monocle", "category": "Cafés", "description": "Modern café open late (until midnight), popular for evening study sessions.", "hours": "07:00-00:00 (All days)", "latitude": 36.807827422383774, "longitude": 10.080482616369578},
            {"name": "Café Dream", "category": "Cafés", "description": "Cozy café in Sidi Amor area.", "latitude": 36.80612666285627, "longitude": 10.083919270352581, "address": "7 Kifah Street, Sidi Amor, Manouba"},
            {"name": "Manouba Tunis", "category": "Cafés", "description": "Neighborhood café offering traditional Tunisian coffee.", "phone": "95315591", "latitude": 36.805245501370884, "longitude": 10.08312545498805},
            {"name": "My First Coffee", "category": "Cafés", "description": "Trendy coffee shop, popular with students for specialty coffees.", "phone": "98260821", "latitude": 36.80499521685056, "longitude": 10.082640620301648},
            {"name": "Wael Coffee", "category": "Cafés", "description": "Local coffee shop offering traditional Tunisian coffee.", "latitude": 36.8124223534867, "longitude": 10.079174738978429},
            {"name": "Restaurant Universitaire La Manouba", "category": "Restaurants", "description": "University canteen offering affordable, subsidized meals for students.", "hours": "Monday-Saturday: 11:30-13:00", "phone": "71601260", "latitude": 36.815234187756985, "longitude": 10.05913406701295},
            {"name": "Jas Green Lounge", "category": "Restaurants", "description": "Green-themed lounge and restaurant.", "hours": "07:00-00:00 (All days)", "latitude": 36.80706208526519, "longitude": 10.083024071335176},
            {"name": "Hassine Jaziri University Restaurant", "category": "Restaurants", "description": "Evening university restaurant serving dinner to students.", "hours": "Monday-Friday: 17:00-19:00", "latitude": 36.8134686498391, "longitude": 10.0674960079498},
            {"name": "Publinet Cite ibn zohr", "category": "Services", "description": "Internet café and computer center offering printing, scanning, internet services.", "hours": "Monday-Sunday: 08:30-20:30", "phone": "21209923", "latitude": 36.81273926119694, "longitude": 10.079056107209711},
            {"name": "Club Robotique ISAMM", "category": "Clubs and Organizations", "description": "Robotics club at ISAMM.", "latitude": 36.81715370710495, "longitude": 10.060801723292018},
            {"name": "Orenda Junior Entreprise", "category": "Clubs and Organizations", "description": "Student-run junior enterprise at ISAMM.", "hours": "Monday-Saturday: 08:00-18:30", "phone": "99750415", "website": "https://orendaje.com/", "latitude": 36.81693600392577, "longitude": 10.060836075619513},
            {"name": "GDG On Campus ISAMM", "category": "Clubs and Organizations", "description": "Google Developer Group at ISAMM.", "latitude": 36.816754703113304, "longitude": 10.06084883442705},
            {"name": "ENSI Junior Entreprise", "category": "Clubs and Organizations", "description": "Prestigious junior enterprise at ENSI offering IT consulting.", "hours": "Monday-Friday: 08:30-19:00", "website": "http://www.ensijuniorentreprise.com/", "latitude": 36.81404417106072, "longitude": 10.063761358771929},
            {"name": "Pépinière Manouba", "category": "Clubs and Organizations", "description": "Business incubator at ISCAE supporting student startups.", "latitude": 36.81561652132451, "longitude": 10.061728419428977},
            {"name": "Foyer Universitaire Bassatine", "category": "University Housing", "description": "University dormitory offering affordable accommodation.", "phone": "71600700", "website": "http://www.flm.rnu.tn/", "latitude": 36.81443647961455, "longitude": 10.066920925140536},
            {"name": "Foyer Universitaire Ibn Zohr Manouba", "category": "University Housing", "description": "Student residence named after the famous physician Ibn Zohr.", "phone": "71600326", "latitude": 36.812755604008125, "longitude": 10.079198096305007},
            {"name": "Foyer Universitaire El Talibet", "category": "University Housing", "description": "University dormitory providing budget-friendly housing.", "phone": "71601200", "latitude": 36.81442321170147, "longitude": 10.067344253975994},
            {"name": "Manouba Technopark", "category": "Technology Park", "description": "Technology park hosting innovative startups and tech companies.", "hours": "Monday-Friday: 08:00-12:30, 14:00-17:30", "phone": "22773481", "latitude": 36.806573383776986, "longitude": 10.073541111646787},
            {"name": "Digit'oz", "category": "Technology Companies", "description": "Digital agency offering web development and IT solutions.", "phone": "55941595", "latitude": 36.80625159681611, "longitude": 10.073679777692377},
            {"name": "Top Digital Agency", "category": "Technology Companies", "description": "Digital marketing and web development agency.", "hours": "Monday-Thursday: 09:00-18:00", "phone": "52817963", "latitude": 36.806687419331, "longitude": 10.073657018395323},
            {"name": "SAMOBAY Digital Solutions", "category": "Technology Companies", "description": "Digital solutions company specializing in software development.", "hours": "Monday-Friday: 09:00-13:00, 14:00-17:30", "phone": "28191220", "website": "https://samobay.tn/", "latitude": 36.8068910637602, "longitude": 10.073724025140201},
            {"name": "SMART-IT", "category": "Technology Companies", "description": "IT company offering smart technology solutions.", "phone": "50740660", "latitude": 36.80679060488733, "longitude": 10.073485625140217},
            {"name": "ZUM-IT", "category": "Technology Companies", "description": "IT services company providing technology consulting.", "phone": "71602410", "website": "https://www.zum-it.com/", "latitude": 36.80736161369256, "longitude": 10.072895382811273},
            {"name": "Salle des Conférences", "category": "Assembly Spaces", "description": "Conference hall in the technopark for seminars and workshops.", "phone": "22773481", "latitude": 36.806510232839166, "longitude": 10.071709461372576},
            {"name": "Carthage Modernity Amphitheater", "category": "Assembly Spaces", "description": "Large university amphitheater for major lectures and ceremonies.", "phone": "71601499", "website": "http://www.uma.rnu.tn/", "latitude": 36.81544463965357, "longitude": 10.061402372623306},
            {"name": "CRISTAL Laboratory GRIFT Group", "category": "Research", "description": "University research laboratory focused on computing and IT.", "latitude": 36.813647754770805, "longitude": 10.064457150096539},
            {"name": "Higher Institute for the Study of Contemporary History of Tunisia", "category": "Research", "description": "Research institute dedicated to studying modern Tunisian history.", "latitude": 36.810125881617196, "longitude": 10.069259343838096},
            {"name": "FLAH Language Library", "category": "Libraries", "description": "Specialized library for language studies at the Faculty of Letters.", "hours": "Monday-Saturday: 08:00-17:00", "latitude": 36.812844350461276, "longitude": 10.065262518736775},
            {"name": "Station Campus Universitaire", "category": "Metro Stations", "description": "Metro station serving the university campus area.", "website": "https://www.transtu.tn/ar/", "latitude": 36.81281304685629, "longitude": 10.061955955301656},
            {"name": "Palais El-Warda", "category": "Metro Stations", "description": "Metro station in the El-Warda palace area.", "latitude": 36.810379998040396, "longitude": 10.067881996304894},
            {"name": "Aboubaker Errazi", "category": "Metro Stations", "description": "Metro station serving the Sidi Amor residential area.", "latitude": 36.80736493603723, "longitude": 10.082301551886486},
            {"name": "Station Campus Manouba", "category": "Metro Stations", "description": "Metro station serving the Manouba campus west side.", "latitude": 36.81550638554843, "longitude": 10.056838197230206},
            {"name": "Mosquée Alyaquin", "category": "Mosques", "description": "Local mosque for daily prayers.", "latitude": 36.818103361109266, "longitude": 10.063701852127501},
            {"name": "Mosquée Errahmen", "category": "Mosques", "description": "Mosque serving the local community.", "latitude": 36.80734444340686, "longitude": 10.082163511646788},
            {"name": "Mosquée Essaida", "category": "Mosques", "description": "Local mosque providing prayer services.", "latitude": 36.80916771268135, "longitude": 10.085713058795813},
            {"name": "Manouba University Dispensary", "category": "Medical Services", "description": "University health clinic providing basic medical services.", "latitude": 36.812509373036974, "longitude": 10.064431198153589},
            {"name": "Centre Médical Public La Manouba", "category": "Medical Services", "description": "Public medical center offering general healthcare services.", "latitude": 36.80705053397314, "longitude": 10.084509394456074},
            {"name": "Centre de Santé Universitaire", "category": "Medical Services", "description": "University health center specifically for students.", "latitude": 36.80761740888506, "longitude": 10.084595205482062},
            {"name": "Baya Gym", "category": "Gym", "description": "Fitness center open daily.", "hours": "07:00-21:00 (All days)", "phone": "53360144", "latitude": 36.80689537516675, "longitude": 10.077279569317843},
            {"name": "Rise Up", "category": "Gym", "description": "Fitness center promoting health and wellness.", "latitude": 36.80385440450327, "longitude": 10.072387220233129},
            {"name": "Manouba Sports Hall", "category": "Sports Facilities", "description": "Indoor sports complex.", "hours": "Monday-Saturday: 09:00-21:00", "latitude": 36.802849311361086, "longitude": 10.069136382984139},
            {"name": "Complexe Sportif Manouba", "category": "Sports Facilities", "description": "Sports complex with multiple facilities.", "phone": "58208696", "latitude": 36.80201601903263, "longitude": 10.07097101385817},
        ]
        for loc_data in locations:
            CampusLocation.objects.create(**loc_data)
        self.stdout.write(f'  Loaded {len(locations)} locations')
