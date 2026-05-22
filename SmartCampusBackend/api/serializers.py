from rest_framework import serializers
from django.contrib.auth.models import User
import mimetypes
import os
from .models import (
    UserProfile, CampusLocation, Event, Report, Favorite, CampusStat,
    Post, PostLike, UserFollow, PostComment, Conversation, Message, Repost, Notification
)


def get_file_name(file_field):
    if not file_field:
        return ''
    return os.path.basename(file_field.name or '')


def get_file_type(file_field):
    if not file_field:
        return ''
    guessed_type, _ = mimetypes.guess_type(file_field.name or '')
    return guessed_type or 'application/octet-stream'


class UserProfileSerializer(serializers.ModelSerializer):
    avatar = serializers.SerializerMethodField()

    class Meta:
        model = UserProfile
        fields = ['university', 'student_id', 'joined_date', 'bio', 'avatar_color', 'avatar', 'role']

    def get_avatar(self, obj):
        request = self.context.get('request')
        if obj.avatar and request:
            return request.build_absolute_uri(obj.avatar.url)
        return None


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
    image_url = serializers.SerializerMethodField()
    is_interested = serializers.SerializerMethodField()
    is_attending = serializers.SerializerMethodField()
    participants_count = serializers.SerializerMethodField()

    class Meta:
        model = Event
        fields = ['id', 'title', 'subtitle', 'description', 'date_display', 'location',
                  'image_name', 'image_url', 'order', 'is_interested', 'is_attending', 'participants_count']

    def get_image_url(self, obj):
        request = self.context.get('request')
        if obj.image_name and request:
            return request.build_absolute_uri(f"/media/event_images/{obj.image_name}.jpg")
        return None

    def get_is_interested(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            return obj.registrations.filter(user=request.user, is_interested=True).exists()
        return False

    def get_is_attending(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            return obj.registrations.filter(user=request.user, is_attending=True).exists()
        return False

    def get_participants_count(self, obj):
        return obj.registrations.filter(is_attending=True).count()


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
    avatar = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ['id', 'first_name', 'last_name', 'role', 'university', 'avatar_color', 'avatar']

    def get_role(self, obj):
        try:
            return obj.profile.role
        except Exception:
            return 'student'

    def get_university(self, obj):
        try:
            return obj.profile.university
        except Exception:
            return ''

    def get_avatar_color(self, obj):
        try:
            return obj.profile.avatar_color
        except Exception:
            return '#1A237E'

    def get_avatar(self, obj):
        request = self.context.get('request')
        try:
            if obj.profile.avatar and request:
                return request.build_absolute_uri(obj.profile.avatar.url)
        except Exception:
            pass
        return None


class UserSearchSerializer(serializers.ModelSerializer):
    """Public-facing user card for the Search tab."""
    role = serializers.SerializerMethodField()
    university = serializers.SerializerMethodField()
    avatar_color = serializers.SerializerMethodField()
    bio = serializers.SerializerMethodField()
    followers_count = serializers.SerializerMethodField()
    following_count = serializers.SerializerMethodField()
    avatar = serializers.SerializerMethodField()
    full_name = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = [
            'id', 'first_name', 'last_name', 'full_name', 'role', 'university',
            'avatar_color', 'avatar', 'bio', 'followers_count', 'following_count'
        ]

    def get_full_name(self, obj):
        name = f"{obj.first_name} {obj.last_name}".strip()
        return name if name else obj.username

    def get_role(self, obj):
        try:
            return obj.profile.role
        except Exception:
            return 'student'

    def get_university(self, obj):
        try:
            return obj.profile.university
        except Exception:
            return ''

    def get_avatar_color(self, obj):
        try:
            return obj.profile.avatar_color
        except Exception:
            return '#1A237E'

    def get_avatar(self, obj):
        request = self.context.get('request')
        try:
            if obj.profile.avatar and request:
                return request.build_absolute_uri(obj.profile.avatar.url)
        except Exception:
            pass
        return None

    def get_bio(self, obj):
        try:
            return obj.profile.bio
        except Exception:
            return ''

    def get_followers_count(self, obj):
        if hasattr(obj, 'followers_count_annotated'):
            return obj.followers_count_annotated
        return obj.followers.count()

    def get_following_count(self, obj):
        if hasattr(obj, 'following_count_annotated'):
            return obj.following_count_annotated
        return obj.following.count()


class PostCommentSerializer(serializers.ModelSerializer):
    author = PostAuthorSerializer(read_only=True)

    class Meta:
        model = PostComment
        fields = ['id', 'author', 'content', 'created_at']


class PostSerializer(serializers.ModelSerializer):
    author = PostAuthorSerializer(read_only=True)
    likes_count = serializers.SerializerMethodField()
    comments_count = serializers.SerializerMethodField()
    first_comment = serializers.SerializerMethodField()
    is_liked_by_me = serializers.SerializerMethodField()
    image_url = serializers.SerializerMethodField()
    file_url = serializers.SerializerMethodField()
    file_name = serializers.SerializerMethodField()
    file_type = serializers.SerializerMethodField()
    repost_of_detail = serializers.SerializerMethodField()

    class Meta:
        model = Post
        fields = [
            'id', 'author', 'content', 'image_url', 'file_url', 'file_name', 'file_type', 'post_type',
            'likes_count', 'comments_count', 'first_comment', 'is_liked_by_me',
            'repost_of', 'repost_of_detail', 'created_at',
        ]
        read_only_fields = ['post_type', 'created_at']

    def get_likes_count(self, obj):
        if hasattr(obj, 'annotated_likes_count'):
            return obj.annotated_likes_count
        return obj.likes.count()

    def get_comments_count(self, obj):
        if hasattr(obj, 'annotated_comments_count'):
            return obj.annotated_comments_count
        return obj.comments.count()

    def get_first_comment(self, obj):
        if hasattr(obj, 'prefetched_comments'):
            comments = obj.prefetched_comments
            if comments:
                return PostCommentSerializer(comments[0], context=self.context).data
            return None
        comment = obj.comments.order_by('created_at').first()
        if comment:
            return PostCommentSerializer(comment, context=self.context).data
        return None

    def get_is_liked_by_me(self, obj):
        if hasattr(obj, 'is_liked'):
            return obj.is_liked
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            return PostLike.objects.filter(user=request.user, post=obj).exists()
        return False

    def get_image_url(self, obj):
        request = self.context.get('request')
        if obj.image and request:
            return request.build_absolute_uri(obj.image.url)
        return None

    def get_file_url(self, obj):
        if obj.external_file_url:
            return obj.external_file_url
        request = self.context.get('request')
        if obj.file and request:
            return request.build_absolute_uri(obj.file.url)
        return None

    def get_file_name(self, obj):
        if obj.external_file_name:
            return obj.external_file_name
        return get_file_name(obj.file)

    def get_file_type(self, obj):
        if obj.external_file_type:
            return obj.external_file_type
        return get_file_type(obj.file)

    def get_repost_of_detail(self, obj):
        if obj.repost_of:
            return PostSerializer(obj.repost_of, context=self.context).data
        return None


class MessageSerializer(serializers.ModelSerializer):
    sender_name = serializers.ReadOnlyField(source='sender.get_full_name')
    sender_id = serializers.ReadOnlyField(source='sender.id')
    image_url = serializers.SerializerMethodField()
    file_url = serializers.SerializerMethodField()
    file_name = serializers.SerializerMethodField()
    file_type = serializers.SerializerMethodField()
    is_me = serializers.SerializerMethodField()

    class Meta:
        model = Message
        fields = ['id', 'sender', 'sender_id', 'sender_name', 'content',
                  'image_url', 'file_url', 'file_name', 'file_type',
                  'timestamp', 'is_read', 'is_me']

    def get_image_url(self, obj):
        request = self.context.get('request')
        if obj.image and request:
            return request.build_absolute_uri(obj.image.url)
        return None

    def get_file_url(self, obj):
        if obj.external_file_url:
            return obj.external_file_url
        request = self.context.get('request')
        if obj.file and request:
            return request.build_absolute_uri(obj.file.url)
        return None

    def get_file_name(self, obj):
        if obj.external_file_name:
            return obj.external_file_name
        return get_file_name(obj.file)

    def get_file_type(self, obj):
        if obj.external_file_type:
            return obj.external_file_type
        return get_file_type(obj.file)

    def get_is_me(self, obj):
        request = self.context.get('request')
        if request:
            return obj.sender == request.user
        return False


class ConversationSerializer(serializers.ModelSerializer):
    participants = UserSearchSerializer(many=True, read_only=True)
    last_message = serializers.SerializerMethodField()
    unread_count = serializers.SerializerMethodField()

    class Meta:
        model = Conversation
        fields = ['id', 'participants', 'last_message', 'unread_count', 'updated_at']

    def get_last_message(self, obj):
        if hasattr(obj, 'prefetched_messages'):
            msgs = obj.prefetched_messages
            if msgs:
                return MessageSerializer(msgs[-1], context=self.context).data
            return None
        msg = obj.messages.last()
        if msg:
            return MessageSerializer(msg, context=self.context).data
        return None

    def get_unread_count(self, obj):
        if hasattr(obj, 'annotated_unread_count'):
            return obj.annotated_unread_count
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            return obj.messages.filter(is_read=False).exclude(sender=request.user).count()
        return 0


class NotificationSerializer(serializers.ModelSerializer):
    sender_name = serializers.SerializerMethodField()
    sender_avatar = serializers.SerializerMethodField()

    class Meta:
        model = Notification
        fields = ['id', 'notification_type', 'title', 'body', 'is_read', 'created_at',
                  'sender_name', 'sender_avatar']

    def get_sender_name(self, obj):
        if obj.sender:
            name = f"{obj.sender.first_name} {obj.sender.last_name}".strip()
            return name if name else obj.sender.username
        return ''

    def get_sender_avatar(self, obj):
        request = self.context.get('request')
        if obj.sender and request:
            try:
                if obj.sender.profile.avatar:
                    return request.build_absolute_uri(obj.sender.profile.avatar.url)
            except Exception:
                pass
        return None
