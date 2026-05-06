from rest_framework import status
from rest_framework.decorators import api_view, permission_classes, parser_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.authtoken.models import Token
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser
from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from django.db.models import Q

from .models import CampusLocation, Event, Report, Favorite, CampusStat, Post, PostLike, UserFollow, Message, Conversation, PostComment, EventRegistration
from .serializers import (
    UserSerializer, RegisterSerializer, LoginSerializer,
    CampusLocationSerializer, EventSerializer, ReportSerializer,
    FavoriteSerializer, CampusStatSerializer,
    PostSerializer, UserSearchSerializer,
    PostCommentSerializer, MessageSerializer, ConversationSerializer,
)


@api_view(['POST'])
@permission_classes([AllowAny])
def register_view(request):
    serializer = RegisterSerializer(data=request.data)
    if serializer.is_valid():
        user = serializer.save()
        token, _ = Token.objects.get_or_create(user=user)
        return Response({
            'token': token.key,
            'user': UserSerializer(user).data,
        }, status=status.HTTP_201_CREATED)
    
    # Print errors to the console for debugging on Render
    print(f"DEBUG: Registration failed. Errors: {serializer.errors}")
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


@api_view(['POST'])
@permission_classes([AllowAny])
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
    following_users = User.objects.filter(id__in=following_ids)
    serializer = UserSearchSerializer(following_users, many=True, context={'request': request})
    return Response(serializer.data)


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


# ──────────────────────────────────────────
#  Social Hub Views
# ──────────────────────────────────────────

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def social_feed_view(request):
    posts = Post.objects.select_related('author', 'author__profile').all().order_by('-created_at')[:30]
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
    if isinstance(content, list): content = content[0] if content else ''
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
    ).exclude(id=request.user.id).select_related('profile')[:20]

    serializer = UserSearchSerializer(users, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def social_suggestions_view(request):
    followed_ids = UserFollow.objects.filter(follower=request.user).values_list('following_id', flat=True)
    suggestions = User.objects.exclude(
        id=request.user.id
    ).exclude(
        id__in=followed_ids
    ).select_related('profile').order_by('?')[:10]
    
    serializer = UserSearchSerializer(suggestions, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def social_public_profile_view(request, pk):
    try:
        user = User.objects.select_related('profile').get(pk=pk)
    except User.DoesNotExist:
        return Response({'error': 'User not found.'}, status=status.HTTP_404_NOT_FOUND)

    is_following = UserFollow.objects.filter(follower=request.user, following=user).exists()
    user_data = UserSearchSerializer(user, context={'request': request}).data
    user_data['is_following'] = is_following
    
    posts = Post.objects.filter(author=user).order_by('-created_at')[:10]
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
        UserFollow.objects.get_or_create(follower=request.user, following=to_follow)
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
        comments = post.comments.all()
        serializer = PostCommentSerializer(comments, many=True, context={'request': request})
        return Response(serializer.data)

    elif request.method == 'POST':
        content = request.data.get('content', '').strip()
        if not content:
            return Response({'error': 'Content cannot be empty.'}, status=status.HTTP_400_BAD_REQUEST)
        comment = PostComment.objects.create(post=post, author=request.user, content=content)
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
    repost = Post.objects.create(author=request.user, content=content, repost_of=original_post)
    serializer = PostSerializer(repost, context={'request': request})
    return Response(serializer.data, status=status.HTTP_201_CREATED)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def chat_list_view(request):
    conversations = request.user.conversations.all()
    serializer = ConversationSerializer(conversations, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
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
        messages = conversation.messages.all().order_by('timestamp')
        serializer = MessageSerializer(messages, many=True, context={'request': request})
        return Response(serializer.data)

    elif request.method == 'POST':
        content = request.data.get('content', '')
        if content is None: content = ""
        content = str(content).strip()
        
        image = request.FILES.get('image')
        file_obj = request.FILES.get('file')

        if not content and not image and not file_obj:
            return Response({'error': 'Message cannot be empty.'}, status=status.HTTP_400_BAD_REQUEST)
        
        message = Message.objects.create(
            conversation=conversation, 
            sender=request.user, 
            content=content,
            image=image,
            file=file_obj
        )
        serializer = MessageSerializer(message, context={'request': request})
        return Response(serializer.data, status=status.HTTP_201_CREATED)


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
