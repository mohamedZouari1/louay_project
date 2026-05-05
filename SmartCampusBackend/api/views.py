from rest_framework import status
from rest_framework.decorators import api_view, permission_classes, parser_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.authtoken.models import Token
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser
from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from django.db.models import Q

from .models import CampusLocation, Event, Report, Favorite, CampusStat, Post, PostLike, UserFollow
from .serializers import (
    UserSerializer, RegisterSerializer, LoginSerializer,
    CampusLocationSerializer, EventSerializer, ReportSerializer,
    FavoriteSerializer, CampusStatSerializer,
    PostSerializer, UserSearchSerializer,
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
        return Response(UserSerializer(request.user).data)

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

        return Response(UserSerializer(user).data)


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
    serializer = EventSerializer(events, many=True)
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
    """Return the latest 30 posts from all users, newest first."""
    posts = Post.objects.select_related('author', 'author__profile').all()[:30]
    serializer = PostSerializer(posts, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
@parser_classes([MultiPartParser, FormParser, JSONParser])
def social_create_post_view(request):
    """Create a new post. post_type is auto-set from the author's verified role."""
    # In Multipart requests, content might be in request.POST or request.data
    content = request.data.get('content', '')
    if not content and 'content' in request.POST:
        content = request.POST.get('content', '')
    
    content = str(content).strip()
    if not content:
        return Response({'error': 'Content cannot be empty.'}, status=status.HTTP_400_BAD_REQUEST)

    post = Post(author=request.user, content=content)
    if 'image' in request.FILES:
        post.image = request.FILES['image']
    post.save()  # save() auto-sets post_type from author.profile.role

    serializer = PostSerializer(post, context={'request': request})
    return Response(serializer.data, status=status.HTTP_201_CREATED)


@api_view(['POST', 'DELETE'])
@permission_classes([IsAuthenticated])
def social_like_post_view(request, pk):
    """POST to like, DELETE to unlike."""
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
    """Create a text-only post via JSON body (no image)."""
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
    """Search users by name or university. ?q=<query>"""
    q = request.query_params.get('q', '').strip()
    if len(q) < 2:
        return Response([])

    try:
        users = User.objects.filter(
            Q(first_name__icontains=q) |
            Q(last_name__icontains=q) |
            Q(profile__university__icontains=q)
        ).exclude(id=request.user.id).select_related('profile')[:20]
    except Exception:
        users = User.objects.filter(
            Q(first_name__icontains=q) |
            Q(last_name__icontains=q)
        ).exclude(id=request.user.id)[:20]

    serializer = UserSearchSerializer(users, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def social_public_profile_view(request, pk):
    """Get any user's public profile and their last 10 posts."""
    try:
        user = User.objects.select_related('profile').get(pk=pk)
    except User.DoesNotExist:
        return Response({'error': 'User not found.'}, status=status.HTTP_404_NOT_FOUND)

    user_data = UserSearchSerializer(user, context={'request': request}).data
    posts = Post.objects.filter(author=user).order_by('-created_at')[:10]
    posts_data = PostSerializer(posts, many=True, context={'request': request}).data

    return Response({
        'user': user_data,
        'posts': posts_data,
    })
