from rest_framework import serializers
from django.contrib.auth.models import User
from .models import UserProfile, CampusLocation, Event, Report, Favorite, CampusStat


class UserProfileSerializer(serializers.ModelSerializer):
    class Meta:
        model = UserProfile
        fields = ['university', 'student_id', 'joined_date']


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
        UserProfile.objects.create(
            user=user,
            university=validated_data['university'],
            student_id=f'ST{random.randint(1000, 9999)}',
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
