from rest_framework import status
from rest_framework.decorators import api_view, permission_classes, parser_classes, authentication_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.authtoken.models import Token
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser
from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from django.db.models import Q, Count, Exists, OuterRef, Prefetch

from .models import (CampusLocation, Event, Report, Favorite, CampusStat,
                     Post, PostLike, UserFollow, Message, Conversation, PostComment,
                     EventRegistration, Notification)
from .media_uploads import upload_attachment
from .serializers import (
    UserSerializer, RegisterSerializer, LoginSerializer,
    CampusLocationSerializer, EventSerializer, ReportSerializer,
    FavoriteSerializer, CampusStatSerializer,
    PostSerializer, UserSearchSerializer,
    PostCommentSerializer, MessageSerializer, ConversationSerializer,
    NotificationSerializer,
)


# ─────────────────────────────────────────
#  Auth Views
# ─────────────────────────────────────────

@api_view(['POST'])
@permission_classes([AllowAny])
@authentication_classes([])
def register_view(request):
    serializer = RegisterSerializer(data=request.data)
    if serializer.is_valid():
        user = serializer.save()
        token, _ = Token.objects.get_or_create(user=user)
        return Response({
            'token': token.key,
            'user': UserSerializer(user).data,
        }, status=status.HTTP_201_CREATED)

    print(f"DEBUG: Registration failed. Errors: {serializer.errors}")
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


@api_view(['POST'])
@permission_classes([AllowAny])
@authentication_classes([])
def login_view(request):
    serializer = LoginSerializer(data=request.data)
    if serializer.is_valid():
        email = serializer.validated_data['email']
        password = serializer.validated_data['password']
        try:
            user_obj = User.objects.get(email=email)
            user = authenticate(username=user_obj.username, password=password)
        except User.DoesNotExist:
            user = None

        if user:
            token, _ = Token.objects.get_or_create(user=user)
            return Response({
                'token': token.key,
                'user': UserSerializer(user).data,
            })
        return Response({'error': 'Invalid credentials'}, status=status.HTTP_401_UNAUTHORIZED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def logout_view(request):
    request.user.auth_token.delete()
    return Response({'message': 'Logged out successfully'})


# ─────────────────────────────────────────
#  Profile Views
# ─────────────────────────────────────────

@api_view(['GET', 'PUT'])
@permission_classes([IsAuthenticated])
def profile_view(request):
    if request.method == 'GET':
        return Response(UserSerializer(request.user, context={'request': request}).data)

    elif request.method == 'PUT':
        user = request.user
        user.first_name = request.data.get('first_name', user.first_name)
        user.last_name = request.data.get('last_name', user.last_name)
        user.save()

        profile = user.profile
        if 'university' in request.data:
            profile.university = request.data['university']
        if 'bio' in request.data:
            profile.bio = request.data['bio']
        if 'avatar_color' in request.data:
            profile.avatar_color = request.data['avatar_color']
        profile.save()

        return Response(UserSerializer(user, context={'request': request}).data)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_following_view(request):
    following_ids = request.user.following.values_list('following_id', flat=True)
    following_users = User.objects.filter(id__in=following_ids).select_related('profile').annotate(
        followers_count_annotated=Count('followers', distinct=True),
        following_count_annotated=Count('following', distinct=True)
    )
    serializer = UserSearchSerializer(following_users, many=True, context={'request': request})
    return Response(serializer.data)


# ─────────────────────────────────────────
#  Campus Data Views
# ─────────────────────────────────────────

@api_view(['GET'])
@permission_classes([AllowAny])
def locations_view(request):
    category = request.query_params.get('category', None)
    locations = CampusLocation.objects.all()
    if category:
        locations = locations.filter(category=category)
    serializer = CampusLocationSerializer(locations, many=True)
    return Response(serializer.data)


@api_view(['GET'])
@permission_classes([AllowAny])
def events_view(request):
    events = Event.objects.all()
    serializer = EventSerializer(events, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['GET'])
@permission_classes([AllowAny])
def stats_view(request):
    stats = CampusStat.objects.all()
    serializer = CampusStatSerializer(stats, many=True)
    return Response(serializer.data)


# ─────────────────────────────────────────
#  Reports & Favorites Views
# ─────────────────────────────────────────

@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def reports_view(request):
    if request.method == 'GET':
        reports = Report.objects.filter(user=request.user)
        serializer = ReportSerializer(reports, many=True)
        return Response(serializer.data)

    elif request.method == 'POST':
        serializer = ReportSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(user=request.user)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def favorites_view(request):
    if request.method == 'GET':
        favorites = Favorite.objects.filter(user=request.user)
        serializer = FavoriteSerializer(favorites, many=True)
        return Response(serializer.data)

    elif request.method == 'POST':
        serializer = FavoriteSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save(user=request.user)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


@api_view(['DELETE'])
@permission_classes([IsAuthenticated])
def favorite_delete_view(request, pk):
    try:
        favorite = Favorite.objects.get(pk=pk, user=request.user)
        favorite.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
    except Favorite.DoesNotExist:
        return Response(status=status.HTTP_404_NOT_FOUND)


# ─────────────────────────────────────────
#  Social Hub Views
# ─────────────────────────────────────────

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def social_feed_view(request):
    comments_queryset = PostComment.objects.select_related('author', 'author__profile')
    posts = Post.objects.select_related(
        'author', 'author__profile', 'repost_of', 'repost_of__author', 'repost_of__author__profile'
    ).prefetch_related(
        Prefetch('comments', queryset=comments_queryset, to_attr='prefetched_comments')
    ).annotate(
        annotated_likes_count=Count('likes', distinct=True),
        annotated_comments_count=Count('comments', distinct=True),
        is_liked=Exists(
            PostLike.objects.filter(user=request.user, post=OuterRef('pk'))
        )
    ).all().order_by('-created_at')[:50]
    serializer = PostSerializer(posts, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
@parser_classes([MultiPartParser, FormParser, JSONParser])
def social_create_post_view(request):
    content = request.data.get('content', '')
    if not content and 'content' in request.POST:
        content = request.POST.get('content', '')

    content = str(content).strip()
    if not content:
        return Response({'error': 'Content cannot be empty.'}, status=status.HTTP_400_BAD_REQUEST)

    post = Post(author=request.user, content=content)
    if 'image' in request.FILES:
        post.image = request.FILES['image']
    if 'file' in request.FILES:
        file_obj = request.FILES['file']
        try:
            uploaded = upload_attachment(file_obj, "smartcampus/post_files")
        except Exception as exc:
            return Response(
                {'error': f'Could not upload attachment: {exc}'},
                status=status.HTTP_502_BAD_GATEWAY
            )
        if uploaded:
            post.external_file_url = uploaded['url']
            post.external_file_name = uploaded['name']
            post.external_file_type = uploaded['type']
        else:
            post.file = file_obj
    post.save()

    serializer = PostSerializer(post, context={'request': request})
    return Response(serializer.data, status=status.HTTP_201_CREATED)


@api_view(['POST', 'DELETE'])
@permission_classes([IsAuthenticated])
def social_like_post_view(request, pk):
    try:
        post = Post.objects.get(pk=pk)
    except Post.DoesNotExist:
        return Response({'error': 'Post not found.'}, status=status.HTTP_404_NOT_FOUND)

    if request.method == 'POST':
        like, created = PostLike.objects.get_or_create(user=request.user, post=post)
        # Create notification for post author
        if created and post.author != request.user:
            Notification.objects.create(
                recipient=post.author,
                sender=request.user,
                notification_type='like',
                title=f"{request.user.first_name or request.user.username} liked your post",
                body=post.content[:100] if post.content else '',
            )
        return Response({
            'liked': True,
            'likes_count': post.likes.count(),
        }, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)

    elif request.method == 'DELETE':
        PostLike.objects.filter(user=request.user, post=post).delete()
        return Response({
            'liked': False,
            'likes_count': post.likes.count(),
        }, status=status.HTTP_200_OK)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def social_create_text_post_view(request):
    content = request.data.get('content', '')
    if isinstance(content, list):
        content = content[0] if content else ''
    content = str(content).strip()
    if not content:
        return Response({'error': 'Content cannot be empty.'}, status=status.HTTP_400_BAD_REQUEST)
    post = Post(author=request.user, content=content)
    post.save()
    serializer = PostSerializer(post, context={'request': request})
    return Response(serializer.data, status=status.HTTP_201_CREATED)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def social_search_users_view(request):
    q = request.query_params.get('q', '').strip()
    if len(q) < 2:
        return Response([])

    users = User.objects.filter(
        Q(first_name__icontains=q) |
        Q(last_name__icontains=q) |
        Q(username__icontains=q) |
        Q(profile__university__icontains=q)
    ).exclude(id=request.user.id).select_related('profile').annotate(
        followers_count_annotated=Count('followers', distinct=True),
        following_count_annotated=Count('following', distinct=True)
    )[:20]

    serializer = UserSearchSerializer(users, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def social_suggestions_view(request):
    followed_ids = list(UserFollow.objects.filter(follower=request.user).values_list('following_id', flat=True))
    suggestions_qs = User.objects.exclude(
        id=request.user.id
    ).exclude(
        id__in=followed_ids
    ).select_related('profile').annotate(
        followers_count_annotated=Count('followers', distinct=True),
        following_count_annotated=Count('following', distinct=True)
    ).order_by('?')[:10]

    suggestions_list = list(suggestions_qs)

    # Fallback to display users they already follow if there are less than 5 other users
    if len(suggestions_list) < 5:
        needed = 5 - len(suggestions_list)
        existing_ids = [u.id for u in suggestions_list] + [request.user.id]
        extra_users = User.objects.exclude(
            id__in=existing_ids
        ).select_related('profile').annotate(
            followers_count_annotated=Count('followers', distinct=True),
            following_count_annotated=Count('following', distinct=True)
        ).order_by('?')[:needed]
        suggestions_list.extend(list(extra_users))

    serializer = UserSearchSerializer(suggestions_list, many=True, context={'request': request})
    
    # Dynamically inject the follow state
    data_list = []
    for item in serializer.data:
        item['is_following'] = item['id'] in followed_ids
        data_list.append(item)

    return Response(data_list)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def social_public_profile_view(request, pk):
    try:
        user = User.objects.select_related('profile').annotate(
            followers_count_annotated=Count('followers', distinct=True),
            following_count_annotated=Count('following', distinct=True)
        ).get(pk=pk)
    except User.DoesNotExist:
        return Response({'error': 'User not found.'}, status=status.HTTP_404_NOT_FOUND)

    is_following = UserFollow.objects.filter(follower=request.user, following=user).exists()
    user_data = UserSearchSerializer(user, context={'request': request}).data
    user_data['is_following'] = is_following

    comments_queryset = PostComment.objects.select_related('author', 'author__profile')
    posts = Post.objects.filter(author=user).select_related(
        'author', 'author__profile', 'repost_of', 'repost_of__author', 'repost_of__author__profile'
    ).prefetch_related(
        Prefetch('comments', queryset=comments_queryset, to_attr='prefetched_comments')
    ).annotate(
        annotated_likes_count=Count('likes', distinct=True),
        annotated_comments_count=Count('comments', distinct=True),
        is_liked=Exists(
            PostLike.objects.filter(user=request.user, post=OuterRef('pk'))
        )
    ).order_by('-created_at')[:10]
    posts_data = PostSerializer(posts, many=True, context={'request': request}).data

    return Response({
        'user': user_data,
        'posts': posts_data,
    })


@api_view(['POST', 'DELETE'])
@permission_classes([IsAuthenticated])
def social_follow_view(request, pk):
    try:
        to_follow = User.objects.get(pk=pk)
    except User.DoesNotExist:
        return Response({'error': 'User not found.'}, status=status.HTTP_404_NOT_FOUND)

    if to_follow == request.user:
        return Response({'error': 'You cannot follow yourself.'}, status=status.HTTP_400_BAD_REQUEST)

    if request.method == 'POST':
        _, created = UserFollow.objects.get_or_create(follower=request.user, following=to_follow)
        if created:
            Notification.objects.create(
                recipient=to_follow,
                sender=request.user,
                notification_type='follow',
                title=f"{request.user.first_name or request.user.username} started following you",
            )
        return Response({'followed': True}, status=status.HTTP_201_CREATED)

    elif request.method == 'DELETE':
        UserFollow.objects.filter(follower=request.user, following=to_follow).delete()
        return Response({'followed': False}, status=status.HTTP_200_OK)


@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def social_comments_view(request, pk):
    try:
        post = Post.objects.get(pk=pk)
    except Post.DoesNotExist:
        return Response({'error': 'Post not found.'}, status=status.HTTP_404_NOT_FOUND)

    if request.method == 'GET':
        comments = post.comments.select_related('author', 'author__profile').all()
        serializer = PostCommentSerializer(comments, many=True, context={'request': request})
        return Response(serializer.data)

    elif request.method == 'POST':
        content = request.data.get('content', '').strip()
        if not content:
            return Response({'error': 'Content cannot be empty.'}, status=status.HTTP_400_BAD_REQUEST)
        comment = PostComment.objects.create(post=post, author=request.user, content=content)
        # Notify post author
        if post.author != request.user:
            Notification.objects.create(
                recipient=post.author,
                sender=request.user,
                notification_type='comment',
                title=f"{request.user.first_name or request.user.username} commented on your post",
                body=content[:100],
            )
        serializer = PostCommentSerializer(comment, context={'request': request})
        return Response(serializer.data, status=status.HTTP_201_CREATED)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def social_repost_view(request, pk):
    try:
        original_post = Post.objects.get(pk=pk)
    except Post.DoesNotExist:
        return Response({'error': 'Post not found.'}, status=status.HTTP_404_NOT_FOUND)

    content = request.data.get('content', '').strip()
    repost = Post.objects.create(
        author=request.user,
        content=content,
        repost_of=original_post,
        image=original_post.image,
        file=original_post.file
    )
    serializer = PostSerializer(repost, context={'request': request})
    return Response(serializer.data, status=status.HTTP_201_CREATED)


# ─────────────────────────────────────────
#  Chat Views
# ─────────────────────────────────────────

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def chat_list_view(request):
    participants_qs = User.objects.select_related('profile').annotate(
        followers_count_annotated=Count('followers', distinct=True),
        following_count_annotated=Count('following', distinct=True)
    )
    messages_qs = Message.objects.all().order_by('timestamp')
    conversations = request.user.conversations.all().prefetch_related(
        Prefetch('participants', queryset=participants_qs),
        Prefetch('messages', queryset=messages_qs, to_attr='prefetched_messages')
    ).annotate(
        annotated_unread_count=Count(
            'messages',
            filter=Q(messages__is_read=False) & ~Q(messages__sender=request.user)
        )
    ).order_by('-updated_at')
    serializer = ConversationSerializer(conversations, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
@parser_classes([MultiPartParser, FormParser, JSONParser])
def chat_messages_view(request, pk):
    try:
        other_user = User.objects.get(pk=pk)
    except User.DoesNotExist:
        return Response({'error': 'User not found.'}, status=status.HTTP_404_NOT_FOUND)

    conversation = Conversation.objects.filter(participants=request.user).filter(participants=other_user).first()

    if not conversation and request.method == 'POST':
        conversation = Conversation.objects.create()
        conversation.participants.add(request.user, other_user)

    if request.method == 'GET':
        if not conversation:
            return Response([])
        messages = conversation.messages.select_related('sender').all().order_by('timestamp')
        # Mark received messages as read
        messages.filter(is_read=False).exclude(sender=request.user).update(is_read=True)
        serializer = MessageSerializer(messages, many=True, context={'request': request})
        return Response(serializer.data)

    elif request.method == 'POST':
        content = request.data.get('content', '')
        if content is None:
            content = ""
        content = str(content).strip()

        image = request.FILES.get('image')
        file_obj = request.FILES.get('file')

        if not content and not image and not file_obj:
            return Response({'error': 'Message cannot be empty.'}, status=status.HTTP_400_BAD_REQUEST)

        message_kwargs = {
            'conversation': conversation,
            'sender': request.user,
            'content': content,
            'image': image,
        }
        if file_obj:
            try:
                uploaded = upload_attachment(file_obj, "smartcampus/chat_files")
            except Exception as exc:
                return Response(
                    {'error': f'Could not upload attachment: {exc}'},
                    status=status.HTTP_502_BAD_GATEWAY
                )
            if uploaded:
                message_kwargs.update({
                    'external_file_url': uploaded['url'],
                    'external_file_name': uploaded['name'],
                    'external_file_type': uploaded['type'],
                })
            else:
                message_kwargs['file'] = file_obj

        message = Message.objects.create(**message_kwargs)
        # Update conversation timestamp
        conversation.save()

        # Create notification for the recipient
        Notification.objects.create(
            recipient=other_user,
            sender=request.user,
            notification_type='message',
            title=f"New message from {request.user.first_name or request.user.username}",
            body=content[:100] if content else '📎 Attachment',
        )

        serializer = MessageSerializer(message, context={'request': request})
        return Response(serializer.data, status=status.HTTP_201_CREATED)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def chat_unread_count_view(request):
    """Returns count of unread messages sent to the current user."""
    unread = Message.objects.filter(
        conversation__participants=request.user,
        is_read=False
    ).exclude(sender=request.user).count()
    return Response({'unread': unread})


# ─────────────────────────────────────────
#  Notifications Views
# ─────────────────────────────────────────

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def notifications_view(request):
    """Returns latest 30 notifications for the current user."""
    notifications = Notification.objects.filter(recipient=request.user).select_related('sender', 'sender__profile')[:30]
    serializer = NotificationSerializer(notifications, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def notifications_mark_read_view(request):
    """Mark all notifications as read."""
    Notification.objects.filter(recipient=request.user, is_read=False).update(is_read=True)
    return Response({'status': 'ok'})


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def notifications_unread_count_view(request):
    count = Notification.objects.filter(recipient=request.user, is_read=False).count()
    unread_msgs = Message.objects.filter(
        conversation__participants=request.user,
        is_read=False
    ).exclude(sender=request.user).count()
    return Response({'notifications': count, 'messages': unread_msgs, 'total': count + unread_msgs})


# ─────────────────────────────────────────
#  Event Registration
# ─────────────────────────────────────────

@api_view(['POST'])
@permission_classes([IsAuthenticated])
def event_register_view(request, pk):
    try:
        event = Event.objects.get(pk=pk)
    except Event.DoesNotExist:
        return Response({'error': 'Event not found'}, status=status.HTTP_404_NOT_FOUND)

    action = request.data.get('action')
    reg, _ = EventRegistration.objects.get_or_create(user=request.user, event=event)

    if action == 'interested':
        reg.is_interested = not reg.is_interested
    elif action == 'participate':
        reg.is_attending = not reg.is_attending
    elif action == 'cancel':
        reg.is_interested = False
        reg.is_attending = False

    reg.save()
    return Response({
        'is_interested': reg.is_interested,
        'is_attending': reg.is_attending,
        'participants_count': event.registrations.filter(is_attending=True).count()
    })


@api_view(['POST'])
@permission_classes([IsAuthenticated])
@parser_classes([MultiPartParser, FormParser])
def update_profile_image(request):
    user = request.user
    profile = user.profile
    if 'avatar' in request.FILES:
        profile.avatar = request.FILES['avatar']
        profile.save()
        return Response(UserSerializer(user, context={'request': request}).data)
    return Response({'error': 'No image provided'}, status=status.HTTP_400_BAD_REQUEST)


@api_view(['GET'])
@permission_classes([AllowAny])
def ping_view(request):
    try:
        user_count = User.objects.count()
        return Response({
            'status': 'online',
            'database': 'connected',
            'users': user_count,
            'message': 'Smart Campus Backend is working!'
        })
    except Exception as e:
        return Response({
            'status': 'online',
            'database': 'error',
            'error': str(e),
            'message': 'Backend is up but database is unreachable.'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
