from django.db import models
from django.contrib.auth.models import User


class UserProfile(models.Model):
    UNIVERSITY_CHOICES = [
        ('ENSI', 'ENSI - National School of Computer Science'),
        ('ISCAE', 'ISCAE - Higher Institute of Business Administration'),
        ('ISAMM', 'ISAMM - Higher Institute of Multimedia Arts'),
        ('IPSI', 'IPSI - Institute of Press and Information Sciences'),
        ('ISD', 'ISD - Higher Institute of Documentation'),
        ('ESCT', 'ESCT - Higher School of Commerce of Tunis'),
        ('FLAH', 'FLAH - Faculty of Letters and Humanities'),
        ('ESEN', 'ESEN - Higher School of Digital Economy'),
        ('OTHER', 'Other'),
    ]

    ROLE_CHOICES = [
        ('student', 'Student'),
        ('org', 'Organization'),
        ('admin', 'Administration'),
    ]

    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='profile')
    university = models.CharField(max_length=10, choices=UNIVERSITY_CHOICES, default='ENSI')
    student_id = models.CharField(max_length=20, blank=True)
    joined_date = models.DateTimeField(auto_now_add=True)
    # Social Hub additions
    bio = models.TextField(blank=True, default='')
    avatar_color = models.CharField(max_length=7, default='#1A237E')  # hex color
    role = models.CharField(max_length=10, choices=ROLE_CHOICES, default='student')

    def __str__(self):
        return f"{self.user.username} - {self.university} ({self.role})"


class CampusLocation(models.Model):
    CATEGORY_CHOICES = [
        ('Administration', 'Administration'),
        ('Cafés', 'Cafés'),
        ('Restaurants', 'Restaurants'),
        ('Services', 'Services'),
        ('Clubs and Organizations', 'Clubs and Organizations'),
        ('University Housing', 'University Housing'),
        ('Technology Park', 'Technology Park'),
        ('Technology Companies', 'Technology Companies'),
        ('Assembly Spaces', 'Assembly Spaces'),
        ('Research', 'Research'),
        ('Libraries', 'Libraries'),
        ('Metro Stations', 'Metro Stations'),
        ('Mosques', 'Mosques'),
        ('Medical Services', 'Medical Services'),
        ('Gym', 'Gym'),
        ('Sports Facilities', 'Sports Facilities'),
        ('available_taxi', 'Available Taxi'),
    ]

    name = models.CharField(max_length=200)
    category = models.CharField(max_length=50, choices=CATEGORY_CHOICES)
    description = models.TextField(blank=True)
    hours = models.CharField(max_length=200, blank=True)
    phone = models.CharField(max_length=50, blank=True)
    website = models.URLField(max_length=300, blank=True)
    address = models.CharField(max_length=300, blank=True)
    latitude = models.FloatField()
    longitude = models.FloatField()

    class Meta:
        ordering = ['category', 'name']

    def __str__(self):
        return f"{self.name} ({self.category})"


class Event(models.Model):
    title = models.CharField(max_length=200)
    subtitle = models.CharField(max_length=200, blank=True)
    description = models.TextField()
    date_display = models.CharField(max_length=100)
    location = models.CharField(max_length=300, blank=True)
    image_name = models.CharField(max_length=100, blank=True)
    order = models.IntegerField(default=0)

    class Meta:
        ordering = ['order']

    def __str__(self):
        return self.title


class Report(models.Model):
    ISSUE_TYPES = [
        ('facilities', 'Facilities & Infrastructure'),
        ('accessibility', 'Accessibility'),
        ('safety', 'Safety & Security'),
        ('technology', 'Technology & IT'),
        ('other', 'Other'),
    ]

    STATUS_CHOICES = [
        ('pending', 'Pending'),
        ('in_progress', 'In Progress'),
        ('resolved', 'Resolved'),
    ]

    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='reports')
    issue_type = models.CharField(max_length=20, choices=ISSUE_TYPES)
    description = models.TextField()
    location = models.CharField(max_length=200, blank=True)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.get_issue_type_display()} - {self.user.username}"


class Favorite(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='favorites')
    location = models.ForeignKey(CampusLocation, on_delete=models.CASCADE)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('user', 'location')

    def __str__(self):
        return f"{self.user.username} - {self.location.name}"


class CampusStat(models.Model):
    label = models.CharField(max_length=100)
    value = models.CharField(max_length=50)
    icon = models.CharField(max_length=50)
    order = models.IntegerField(default=0)

    class Meta:
        ordering = ['order']

    def __str__(self):
        return f"{self.label}: {self.value}"


# ──────────────────────────────────────────
#  Social Hub Models
# ──────────────────────────────────────────

class Post(models.Model):
    POST_TYPE_CHOICES = [
        ('student', 'Student'),
        ('org', 'Organization'),
        ('admin', 'Administration'),
    ]

    author = models.ForeignKey(User, on_delete=models.CASCADE, related_name='posts')
    content = models.TextField()
    image = models.ImageField(upload_to='posts/', blank=True, null=True)
    # post_type is automatically set from author's role on save
    post_type = models.CharField(max_length=10, choices=POST_TYPE_CHOICES, default='student')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def save(self, *args, **kwargs):
        # Always derive post_type from the author's verified role
        try:
            self.post_type = self.author.profile.role
        except Exception:
            self.post_type = 'student'
        super().save(*args, **kwargs)

    @property
    def likes_count(self):
        return self.likes.count()

    def __str__(self):
        return f"[{self.post_type}] {self.author.username}: {self.content[:50]}"


class PostLike(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='post_likes')
    post = models.ForeignKey(Post, on_delete=models.CASCADE, related_name='likes')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('user', 'post')

    def __str__(self):
        return f"{self.user.username} liked post #{self.post.id}"


class UserFollow(models.Model):
    follower = models.ForeignKey(User, on_delete=models.CASCADE, related_name='following')
    following = models.ForeignKey(User, on_delete=models.CASCADE, related_name='followers')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('follower', 'following')

    def __str__(self):
        return f"{self.follower.username} follows {self.following.username}"
