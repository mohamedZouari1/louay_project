from django.contrib import admin
from .models import UserProfile, CampusLocation, Event, Report, Favorite, CampusStat

admin.site.register(UserProfile)
admin.site.register(CampusLocation)
admin.site.register(Event)
admin.site.register(Report)
admin.site.register(Favorite)
admin.site.register(CampusStat)
