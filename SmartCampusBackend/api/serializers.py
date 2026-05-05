from rest_framework import serializers
from django.contrib.auth.models import User
from .models import UserProfile, CampusLocation, Event, Report, Favorite, CampusStat, Post, PostLike, UserFollow


class UserProfileSerializer(serializers.ModelSerializer):
    class Meta:
        model = UserProfile
        fields = ['university', 'student_id', 'joined_date', 'bio', 'avatar_color', 'role']


class UserSerializer(serializers.ModelSerializer):
    profile = UserProfileSerializer(read_only=True)

    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'first_name', 'last_name', 'profile']


class RegisterSerializer(serializers.Serializer):
    name = serializers.CharField(max_length=150)
    email = serializers.EmailField()
    password = serializers.CharField(min_length=6, write_only=True)
    university = serializers.CharField(max_length=10)

    def validate_email(self, value):
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError("A user with this email already exists.")
        return value

    def create(self, validated_data):
        name_parts = validated_data['name'].split(' ', 1)
        first_name = name_parts[0]
        last_name = name_parts[1] if len(name_parts) > 1 else ''

        user = User.objects.create_user(
            username=validated_data['email'],
            email=validated_data['email'],
            password=validated_data['password'],
            first_name=first_name,
            last_name=last_name,
        )

        import random
        # Generate a pleasant avatar color from a curated palette
        colors = ['#1A237E', '#00695C', '#4527A0', '#1565C0', '#BF360C', '#283593', '#558B2F', '#6A1B9A']
        UserProfile.objects.create(
            user=user,
            university=validated_data['university'],
            student_id=f'ST{random.randint(1000, 9999)}',
            avatar_color=random.choice(colors),
            role='student',
        )

        return user


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField()


class CampusLocationSerializer(serializers.ModelSerializer):
    class Meta:
        model = CampusLocation
        fields = '__all__'


class EventSerializer(serializers.ModelSerializer):
    class Meta:
        model = Event
        fields = '__all__'


class ReportSerializer(serializers.ModelSerializer):
    class Meta:
        model = Report
        fields = ['id', 'issue_type', 'description', 'location', 'status', 'created_at']
        read_only_fields = ['status', 'created_at']


class FavoriteSerializer(serializers.ModelSerializer):
    location_detail = CampusLocationSerializer(source='location', read_only=True)

    class Meta:
        model = Favorite
        fields = ['id', 'location', 'location_detail', 'created_at']
        read_only_fields = ['created_at']


class CampusStatSerializer(serializers.ModelSerializer):
    class Meta:
        model = CampusStat
        fields = '__all__'


# ──────────────────────────────────────────
#  Social Hub Serializers
# ──────────────────────────────────────────

class PostAuthorSerializer(serializers.ModelSerializer):
    """Lightweight author info embedded in every post."""
    role = serializers.SerializerMethodField()
    university = serializers.SerializerMethodField()
    avatar_color = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ['id', 'first_name', 'last_name', 'role', 'university', 'avatar_color']

    def get_role(self, obj):
        try: return obj.profile.role
        except: return 'student'

    def get_university(self, obj):
        try: return obj.profile.university
        except: return ''

    def get_avatar_color(self, obj):
        try: return obj.profile.avatar_color
        except: return '#1A237E'


class PostSerializer(serializers.ModelSerializer):
    author = PostAuthorSerializer(read_only=True)
    likes_count = serializers.SerializerMethodField()
    is_liked_by_me = serializers.SerializerMethodField()
    image_url = serializers.SerializerMethodField()

    class Meta:
        model = Post
        fields = [
            'id', 'author', 'content', 'image_url',
            'post_type', 'likes_count', 'is_liked_by_me', 'created_at',
        ]
        read_only_fields = ['post_type', 'created_at']

    def get_likes_count(self, obj):
        return obj.likes.count()

    def get_is_liked_by_me(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            return PostLike.objects.filter(user=request.user, post=obj).exists()
        return False

    def get_image_url(self, obj):
        request = self.context.get('request')
        if obj.image and request:
            return request.build_absolute_uri(obj.image.url)
        return None


class UserSearchSerializer(serializers.ModelSerializer):
    """Public-facing user card for the Search tab."""
    role = serializers.SerializerMethodField()
    university = serializers.SerializerMethodField()
    avatar_color = serializers.SerializerMethodField()
    bio = serializers.SerializerMethodField()
    followers_count = serializers.SerializerMethodField()
    following_count = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = [
            'id', 'first_name', 'last_name', 'role', 'university',
            'avatar_color', 'bio', 'followers_count', 'following_count'
        ]

    def get_role(self, obj):
        try: return obj.profile.role
        except: return 'student'

    def get_university(self, obj):
        try: return obj.profile.university
        except: return ''

    def get_avatar_color(self, obj):
        try: return obj.profile.avatar_color
        except: return '#1A237E'

    def get_bio(self, obj):
        try: return obj.profile.bio
        except: return ''

    def get_followers_count(self, obj):
        return obj.followers.count()

    def get_following_count(self, obj):
        return obj.following.count()
