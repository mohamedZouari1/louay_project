// University Floor Data with GPS Coordinates
// This file contains floor-by-floor facility information for all universities
// Each facility includes name, type, and GPS coordinates for map display

// University image paths
const universityImages = {
  'ENSI': 'ensi.jpg',
  'ESCT': 'esct.jpg',
  'ESEN': 'esen.webp',
  'FLAH': 'flah.jpg',
  'IPSI': 'ipsi.jpg',
  'ISAMM': 'isamm.png',
  'ISCAE': 'iscae.jpg',
  'ISD': 'isd.jpg'
};

// University floor data structure
const universityFloorData = {
  "ENSI": {
    "fullName": "National School of Computer Science (ENSI)",
    "description": "Tunisia's premier institution for computer engineering. Founded in 1984, it trains elite engineers in software engineering, AI, and cybersecurity. The campus features modern labs, high-tech amphitheaters, and vibrant student life areas.",
    "hours": "Monday-Friday: 08:00-18:00 | Saturday: 08:30-13:00 | Sunday: Closed",
    "phone": "+216 71 600 444",
    "website": "http://www.ensi.rnu.tn/",
    "address": "Campus Universitaire de Manouba, 2010 Manouba",
    "floors": [
      {
        "name": "Ground Floor",
        "facilities": [
          { "name": "Volleyball Court", "type": "sports", "lat": 36.81365051166345, "lon": 10.063705060253282, "image": "../ENSI%20volley%20ball%20court.jpeg", "description": "Outdoor area for sports and teamwork activities." },
          { "name": "Basketball Court", "type": "sports", "lat": 36.81348731319031, "lon": 10.063931706909752, "image": "../ENSI%20basket%20ball%20court.jpeg", "description": "High-standard basketball court for students." },
          { "name": "Buvette", "type": "buvette", "lat": 36.8138321540056, "lon": 10.064162134925212, "images": ["../buvette1.jpeg", "../buvette2.jpeg"], "description": "Student cafeteria for breaks, meals, and socializing." },
          { "name": "Clubs Local", "type": "club", "lat": 36.81325069031611, "lon": 10.063561146713319, "image": "../clubs'%20local.jpeg", "description": "Dedicated space for student organizations and innovation." },
          { "name": "Prayer Room", "type": "prayer", "lat": 36.81383047458487, "lon": 10.06368251667694, "description": "Quiet space for reflection and prayer." },
          { "name": "Boys' Toilet (GF)", "type": "toilet", "lat": 36.81380416964988, "lon": 10.063717385388406, "description": "Sanitary facilities for male students." },
          { "name": "Girls' Toilet (GF)", "type": "toilet", "lat": 36.813995282826035, "lon": 10.06385686025415, "description": "Sanitary facilities for female students." },
          { "name": "Labs", "type": "laboratory", "lat": 36.81363342465081, "lon": 10.064247054158349, "image": "../ENSI%20labs.jpeg", "description": "State-of-the-art computer labs for practical training." }
        ]
      },
      {
        "name": "Floor 1",
        "facilities": [
          { "name": "ENSI Entrance", "type": "entrance", "lat": 36.813392790359956, "lon": 10.063447925557249, "image": "../ENSI%20entrance.jpeg", "description": "The main gateway to Tunisia's leading IT engineering school." },
          { "name": "Amphitheater", "type": "academic", "lat": 36.813586628092104, "lon": 10.063658792150468, "images": ["../ENSI%20amphitheater.jpg", "../ENSI%20amphitheater%20entrance.jpeg"], "description": "Main lecture hall for conferences and major classes." },
          { "name": "Library", "type": "library", "lat": 36.81336879487505, "lon": 10.063825344299303, "images": ["../ENSI%20library.jpeg", "../ENSI%20library%20entrance.jpeg"], "description": "Spacious library with a vast collection of computer science resources." },
          { "name": "ENSI Administration", "type": "administration", "lat": 36.81325069031611, "lon": 10.063561146713319, "image": "../ENSI%20administration.jpeg", "description": "Administrative offices and student services." },
          { "name": "MSE Administration", "type": "administration", "lat": 36.81347347830722, "lon": 10.063898434488221, "image": "../MSE%20administration.jpeg", "description": "Administrative section for MSE programs." },
          { "name": "Boys' Toilet (F1)", "type": "toilet", "lat": 36.81380416964988, "lon": 10.063717385388406, "description": "Sanitary facilities on the first floor." },
          { "name": "Girls' Toilet (F1)", "type": "toilet", "lat": 36.81383047458487, "lon": 10.06368251667694, "description": "Sanitary facilities for women on Floor 1." }
        ]
      }
    ]
  },
  "ESCT": {
    "fullName": "École Supérieure de Commerce de Tunis",
    "description": "Higher School of Business in Tunis, specializing in business administration, management, and commercial sciences. Prepares future business leaders and entrepreneurs.",
    "hours": "Monday-Friday: 08:00-17:00 | Saturday-Sunday: Closed",
    "phone": "",
    "website": "http://www.esct.rnu.tn/",
    "address": "University Campus, Manouba",
    "floors": [
      {
        "name": "Ground Floor",
        "facilities": [
          { "name": "Main Entrance", "type": "entrance", "lat": 36.81422, "lon": 10.06213 },
          { "name": "Administration", "type": "administration", "lat": 36.81425, "lon": 10.06215 },
          { "name": "Library", "type": "library", "lat": 36.81430, "lon": 10.06220 }
        ]
      }
    ]
  },
  "ISCAE": {
    "fullName": "Institut Supérieur de Comptabilité et d'Administration des Entreprises",
    "description": "Higher Institute of Accounting and Business Administration. Premier institution for accounting, finance, and business management studies in Tunisia.",
    "hours": "Monday-Saturday: 08:00-18:00 | Sunday: Closed",
    "phone": "71600588",
    "website": "http://www.iscae.rnu.tn/",
    "address": "University Campus, Sanhaja Area, Manouba",
    "floors": [
      {
        "name": "Ground Floor",
        "facilities": [
          { "name": "Main Entrance", "type": "entrance", "lat": 36.81493, "lon": 10.06089 },
          { "name": "Administration", "type": "administration", "lat": 36.81495, "lon": 10.06091 }
        ]
      }
    ]
  },
  "ISAMM": {
    "fullName": "Institut Supérieur des Arts Multimédias de la Manouba",
    "description": "Higher Institute of Multimedia Arts, offering programs in digital arts, multimedia design, animation, and interactive media. Hub for creative technology education.",
    "hours": "Monday-Saturday: 08:30-17:30 | Sunday: Closed",
    "phone": "71601903",
    "website": "http://www.isamm.rnu.tn/",
    "address": "University Campus, Manouba 2010",
    "floors": [
      {
        "name": "Ground Floor",
        "facilities": [
          { "name": "Main Entrance", "type": "entrance", "lat": 36.81701, "lon": 10.06086 },
          { "name": "Administration", "type": "administration", "lat": 36.81703, "lon": 10.06088 }
        ]
      }
    ]
  },
  "FLAH": {
    "fullName": "Faculté des Lettres, des Arts et des Humanités",
    "description": "Faculty of Letters, Arts and Humanities. Offers diverse programs in languages, literature, philosophy, history, and human sciences.",
    "hours": "Monday-Saturday: 08:00-17:00 | Sunday: Closed",
    "phone": "71600811",
    "website": "http://www.flm.rnu.tn/",
    "address": "University Campus, Manouba",
    "floors": [
      {
        "name": "Ground Floor",
        "facilities": [
          { "name": "Main Entrance", "type": "entrance", "lat": 36.81212, "lon": 10.06613 },
          { "name": "Administration", "type": "administration", "lat": 36.81215, "lon": 10.06615 }
        ]
      }
    ]
  },
  "IPSI": {
    "fullName": "Institut de Presse et des Sciences de l'Information",
    "description": "Institute of Press and Information sciences. Training journalists, communication professionals, and media specialists with programs in journalism, public relations, and digital media.",
    "hours": "Monday-Saturday: 08:30-17:00 | Sunday: Closed",
    "phone": "71602919",
    "website": "http://www.ipsi.rnu.tn/",
    "address": "University Campus, Manouba",
    "floors": [
      {
        "name": "Ground Floor",
        "facilities": [
          { "name": "Main Entrance", "type": "entrance", "lat": 36.81102, "lon": 10.06789 },
          { "name": "Administration", "type": "administration", "lat": 36.81105, "lon": 10.06791 }
        ]
      }
    ]
  },
  "ISD": {
    "fullName": "Institut Supérieur de Documentation",
    "description": "Higher Institute of Documentation and Library Sciences. Specializes in information science, library management, archival studies, and knowledge management.",
    "hours": "Monday-Friday: 08:00-17:00 | Saturday: 08:00-12:00 | Sunday: Closed",
    "phone": "71600977",
    "website": "http://www.isd.rnu.tn/",
    "address": "University Campus, Manouba",
    "floors": [
      {
        "name": "Ground Floor",
        "facilities": [
          { "name": "Main Entrance", "type": "entrance", "lat": 36.81059, "lon": 10.06879 },
          { "name": "Library", "type": "library", "lat": 36.81061, "lon": 10.06881 }
        ]
      }
    ]
  },
  "ESEN": {
    "fullName": "École Supérieure d'Économie Numérique",
    "description": "Higher School of Digital Economy. Focuses on digital economics, e-business, and technology-driven business models in the modern economy.",
    "hours": "Monday-Friday: 08:00-17:30 | Saturday-Sunday: Closed",
    "phone": "71609866",
    "website": "http://www.esen.tn/",
    "address": "Manouba Technopark, Digital Economy Zone",
    "floors": [
      {
        "name": "Ground Floor",
        "facilities": [
          { "name": "Main Entrance", "type": "entrance", "lat": 36.80763, "lon": 10.07310 },
          { "name": "Administration", "type": "administration", "lat": 36.80765, "lon": 10.07312 }
        ]
      }
    ]
  }
};

// Facility type icon mapping
const facilityIcons = {
  "entrance": "🚪",
  "administration": "🏢",
  "library": "📚",
  "toilet": "🚻",
  "laboratory": "🔬",
  "buvette": "☕",
  "academic": "🎓",
  "club": "👥",
  "office": "💼",
  "prayer": "🕌",
  "sports": "🏆",
  "yard": "🌳"
};
