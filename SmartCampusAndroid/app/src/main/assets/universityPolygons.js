// University Polygon Map System
// This script adds university building polygons with floor-based indoor mapping

// University Colors
const universityColors = {
    "ENSI": "#A51C30",     // Crimson Red
    "ESCT": "#1565C0",     // Navy Blue
    "ISCAE": "#2E7D32",    // Emerald Green
    "ISAMM": "#7B1FA2",    // Purple
    "FLAH": "#F57C00",     // Orange
    "IPSI": "#00796B",     // Teal
    "ISD": "#E64A19",      // Deep Orange
    "ESEN": "#3949AB"      // Indigo
};

// Global variables
let universityLayer = null;
let currentFloorMarkers = null;
let currentSelectedUniversity = null;
let currentSelectedFloor = null;

// Initialize university polygons when map is ready
function initializeUniversityPolygons() {
    // Load universities.geojson
    fetch('universities.geojson')
        .then(response => response.json())
        .then(data => {
            // Create GeoJSON layer with custom styling
            universityLayer = L.geoJSON(data, {
                style: function (feature) {
                    return getUniversityStyle(feature);
                },
                onEachFeature: function (feature, layer) {
                    setupUniversityInteractions(feature, layer);
                },
                className: 'university-polygon'
            }).addTo(map);

            console.log('✅ University polygons loaded successfully');
        })
        .catch(error => {
            console.error('❌ Error loading universities:', error);
        });
}

// Get style for university polygon
function getUniversityStyle(feature) {
    return {
        fillColor: 'transparent',  // Completely transparent interior
        fillOpacity: 0,
        color: '#000000',          // Unified Black Outline
        weight: 3,
        opacity: 0.8
    };
}

// Setup interactions for each university polygon
function setupUniversityInteractions(feature, layer) {
    const universityName = feature.properties.Names;
    const color = universityColors[universityName] || '#6366f1';

    // Bind tooltip with university name
    layer.bindTooltip(universityName, {
        permanent: false,
        direction: 'center',
        className: 'university-tooltip',
        opacity: 0.9
    });

    // Hover effects  
    layer.on('mouseover', function (e) {
        e.target.setStyle({
            weight: 5,
            opacity: 1.0,
            color: '#000000' // Keep black on hover, just thicker
        });
    });

    layer.on('mouseout', function (e) {
        // Only reset if this isn't the selected university
        if (currentSelectedUniversity !== universityName) {
            e.target.setStyle({
                weight: 3,
                opacity: 0.8,
                color: '#000000'
            });
        }
    });

    // Click to show details in sidebar
    layer.on('click', function (e) {
        showUniversityDetails(universityName, feature, layer);
    });
}

// Open university details in sidebar (not modal)
function showUniversityDetails(universityName, feature, layer) {
    const floorData = universityFloorData[universityName];

    if (!floorData) {
        console.warn(`No floor data found for ${universityName}`);
        return;
    }

    // Store current selection
    currentSelectedUniversity = universityName;

    // Highlight selected polygon
    layer.setStyle({
        weight: 5,
        opacity: 1.0,
        color: '#000000' // Keep black
    });

    // Hide other markers for isolation
    if (window.markersLayer && map.hasLayer(window.markersLayer)) {
        map.removeLayer(window.markersLayer);
    }

    // Zoom map to university - Level 20 for better detail
    if (feature.geometry.type === 'MultiPolygon' && feature.geometry.coordinates.length > 0) {
        const bounds = layer.getBounds();
        map.setView(bounds.getCenter(), 20);
    } else if (feature.geometry.type === 'Polygon') {
        const bounds = layer.getBounds();
        map.setView(bounds.getCenter(), 20);
    }

    // Show in location details sidebar
    showUniversitySidebarDetails({
        name: floorData.fullName || universityName,
        category: 'University',
        description: floorData.description || 'Campus building with multiple floors and facilities.',
        hours: floorData.hours || '',
        phone: floorData.phone || '',
        website: floorData.website || '',
        address: floorData.address || '',
        universityCode: universityName,
        floors: floorData.floors
    });
}

// Show university details in sidebar with TABS
function showUniversitySidebarDetails(location, defaultTab = 'details') {
    const detailsSection = document.getElementById('locationDetailsSection');
    const detailsContent = document.getElementById('locationDetailsContent');

    // Centralized Marker Isolation: Hide campus markers when university details are shown
    if (window.markersLayer && map.hasLayer(window.markersLayer)) {
        map.removeLayer(window.markersLayer);
    }

    let html = '';

    // Add tabs if this is a university
    if (location.universityCode && location.floors) {
        html += `
            <div class="university-tabs">
                <button class="tab-btn ${defaultTab === 'details' ? 'active' : ''}" onclick="switchUniversityTab('details', event)">
                    <i class="fas fa-info-circle"></i> Location Details
                </button>
                <button class="tab-btn ${defaultTab === 'places' ? 'active' : ''}" onclick="switchUniversityTab('places', event)">
                    <i class="fas fa-layer-group"></i> Important Places
                </button>
            </div>
        `;

        // Tab 1: Location Details (FULL ORIGINAL CONTENT WITH IMAGE)
        const universityImage = universityImages[location.universityCode];
        html += `
            <div id="detailsTab" class="tab-content ${defaultTab === 'details' ? 'active' : ''}">
                ${universityImage ? `<img src="${universityImage}" alt="${location.name}" class="university-image" style="width: 100%; border-radius: 12px; margin-bottom: 1rem;">` : ''}
                <h3 class="details-title">${location.name}</h3>
                <span class="details-category">${location.category}</span>
                <p class="details-description">${location.description}</p>
                
                <div class="details-info">
                    ${location.hours ? `
                        <div class="info-item">
                            <i class="fas fa-clock"></i>
                            <div>
                                <strong>Hours:</strong>
                                <p>${location.hours}</p>
                            </div>
                        </div>
                    ` : ''}
                    
                    ${location.phone ? `
                        <div class="info-item">
                            <i class="fas fa-phone"></i>
                            <div>
                                <strong>Phone:</strong>
                                <p><a href="tel:${location.phone}">${location.phone}</a></p>
                            </div>
                        </div>
                    ` : ''}
                    
                    ${location.website ? `
                        <div class="info-item">
                            <i class="fas fa-globe"></i>
                            <div>
                                <strong>Website:</strong>
                                <p><a href="${location.website}" target="_blank">${location.website}</a></p>
                            </div>
                        </div>
                    ` : ''}
                    
                    ${location.address ? `
                        <div class="info-item">
                            <i class="fas fa-map-marker-alt"></i>
                            <div>
                                <strong>Address:</strong>
                                <p>${location.address}</p>
                            </div>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;

        // Tab 2: Important Places (FLOOR SELECTOR)
        html += `
            <div id="placesTab" class="tab-content ${defaultTab === 'places' ? 'active' : ''}">
                <h3 style="margin-bottom: 1.5rem; color: var(--primary-color);">Select a Floor</h3>
                <div class="floor-selector-sidebar">
        `;

        location.floors.forEach((floor, index) => {
            html += `
                <button class="floor-btn-sidebar" onclick="selectUniversityFloor('${location.universityCode}', ${index})">
                    <i class="fas fa-building"></i>
                    <span>${floor.name}</span>
                    <small>${floor.facilities ? floor.facilities.length : 0} places</small>
                </button>
            `;
        });

        html += `
                </div>
            </div>
        `;
    } else {
        // Regular location (not a university) - no tabs
        html += `
            <h3 class="details-title">${location.name}</h3>
            <span class="details-category">${location.category}</span>
            <p class="details-description">${location.description || ''}</p>
        `;
    }

    detailsContent.innerHTML = html;
    detailsSection.style.display = 'block';

    // Add show-details class to sidebar
    document.querySelector('.sidebar').classList.add('show-details');
}

// Switch between tabs
function switchUniversityTab(tabName, event) {
    // Update tab buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // If event is provided, use target. Otherwise find button by onclick attribute content
    let targetBtn = event ? event.currentTarget : null; // Use currentTarget to ensure we get the button, not an icon inside
    if (!targetBtn) {
        // Find button that calls this tab
        const buttons = document.querySelectorAll('.tab-btn');
        for (const btn of buttons) {
            if (btn.getAttribute('onclick') && btn.getAttribute('onclick').includes(`'${tabName}'`)) {
                targetBtn = btn;
                break;
            }
        }
    }

    if (targetBtn) {
        targetBtn.classList.add('active');
    }

    // Update tab content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });

    if (tabName === 'details') {
        document.getElementById('detailsTab').classList.add('active');
    } else if (tabName === 'places') {
        document.getElementById('placesTab').classList.add('active');
    }
}


// Select a specific floor (called from sidebar)
function selectUniversityFloor(universityName, floorIndex) {
    const floorData = universityFloorData[universityName];
    if (!floorData) return;

    // If floorIndex is -1, just show the floor selector (Back button logic)
    if (floorIndex === -1) {
        showUniversitySidebarDetails({
            name: floorData.fullName || universityName,
            category: 'University',
            description: floorData.description || 'Campus building with multiple floors and facilities.',
            hours: floorData.hours || '',
            phone: floorData.phone || '',
            website: floorData.website || '',
            address: floorData.address || '',
            universityCode: universityName,
            floors: floorData.floors
        }, 'places');

        hideFloorMarkers();
        return;
    }

    if (!floorData.floors[floorIndex]) return;

    const floor = floorData.floors[floorIndex];
    currentSelectedFloor = floor;

    // Update active floor button in sidebar
    document.querySelectorAll('.floor-btn-sidebar').forEach((btn, idx) => {
        btn.classList.toggle('active', idx === floorIndex);
    });

    // Zoom to university and show floor markers
    displayFloorMarkers(universityName, floor);

    // Render facility list in sidebar
    renderFacilityList(universityName, floor);

    console.log(`Selected ${universityName} - ${floor.name}`);
}

// Render the list of facilities for the selected floor in the sidebar
function renderFacilityList(universityName, floor) {
    let html = `
        <div class="facility-list-header">
            <h4>${floor.name} - Facilities</h4>
            <button class="back-to-floors" onclick="selectUniversityFloor('${universityName}', -1)">
                <i class="fas fa-chevron-left"></i> Back
            </button>
        </div>
        <div class="facility-items-container">
    `;

    if (!floor.facilities || floor.facilities.length === 0) {
        html += '<p class="empty-message">No specific facilities registered for this floor.</p>';
    } else {
        floor.facilities.forEach(facility => {
            const icon = facilityIcons[facility.type] || '📍';
            const color = universityColors[universityName] || '#6366f1';
            const facilityId = `facility-${facility.name.replace(/\s+/g, '-')}`;

            html += `
                <div class="facility-card" id="${facilityId}" onclick="zoomToFacility([${facility.lat}, ${facility.lon}], '${facility.name.replace(/'/g, "\\'")}')">
                    <div class="facility-card-header">
                        <div class="facility-card-icon" style="background: ${color}20; color: ${color};">
                            <span>${icon}</span>
                        </div>
                        <div class="facility-card-info">
                            <h5>${facility.name}</h5>
                            <p>${facility.description || 'Special place in ENSI'}</p>
                        </div>
                    </div>
                    ${(facility.images || (facility.image ? [facility.image] : [])).length > 0 ? `
                        <div class="facility-image-slider">
                            ${(facility.images || [facility.image]).map(img => `
                                <div class="facility-image-slide" onclick="event.stopPropagation(); openImageModal('${img}')">
                                    <img src="${img}" alt="${facility.name}">
                                    <div class="expand-hint"><i class="fas fa-expand-alt"></i></div>
                                </div>
                            `).join('')}
                        </div>
                    ` : ''}
                </div>
            `;
        });
    }

    html += '</div>';

    // Update the places tab content specifically
    const placesTab = document.getElementById('placesTab');
    if (placesTab) {
        placesTab.innerHTML = html;
    }
}

// Zoom to a specific facility marker
function zoomToFacility(coords, name) {
    map.setView(coords, 20);
    // Popups are removed, so we just zoom
}

// Display markers for facilities on the selected floor
function displayFloorMarkers(universityName, floor) {
    // Remove existing floor markers
    hideFloorMarkers();

    // Create new layer group for this floor's markers
    currentFloorMarkers = L.layerGroup();

    floor.facilities.forEach(facility => {
        if (facility.lat && facility.lon) {
            const icon = facilityIcons[facility.type] || '📍';
            const color = universityColors[universityName] || '#6366f1';

            // Create custom div icon
            const customIcon = L.divIcon({
                className: 'floor-facility-marker',
                html: `<div style="
                    background: ${color};
                    color: white;
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 18px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                    border: 3px solid white;
                ">${icon}</div>`,
                iconSize: [32, 32],
                iconAnchor: [16, 16]
            });

            const marker = L.marker([facility.lat, facility.lon], {
                icon: customIcon
            });

            // Popup removed as per user request
            // marker.bindPopup(...) removed

            currentFloorMarkers.addLayer(marker);

            // Add click event to sync with sidebar
            marker.on('click', function () {
                highlightFacilityInSidebar(facility.name);
            });
        }
    });

    // Add layer to map
    currentFloorMarkers.addTo(map);
}

// Highlight a facility in the sidebar and scroll to it
function highlightFacilityInSidebar(facilityName) {
    // Ensure sidebar details are visible
    const detailsSection = document.getElementById('locationDetailsSection');
    if (detailsSection.style.display !== 'block') {
        detailsSection.style.display = 'block';
        document.querySelector('.sidebar').classList.add('show-details');
    }

    // Ensure we are on the "Important Places" tab
    switchUniversityTab('places');

    const facilityId = `facility-${facilityName.replace(/\s+/g, '-')}`;
    const element = document.getElementById(facilityId);

    if (element) {
        // Remove active class from all cards
        document.querySelectorAll('.facility-card').forEach(card => {
            card.classList.remove('active');
        });

        // Add active class and scroll
        element.classList.add('active');
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });

        // Remove highlight after a few seconds
        setTimeout(() => {
            element.classList.remove('active');
        }, 3000);
    }
}

// Hide floor markers
function hideFloorMarkers() {
    if (currentFloorMarkers) {
        map.removeLayer(currentFloorMarkers);
        currentFloorMarkers = null;
    }
}

// Open Image in Modal
function openImageModal(src) {
    const modal = document.getElementById('imageModal');
    const modalImg = document.getElementById('expandedImage');

    if (modal && modalImg) {
        modal.style.display = "block";
        modalImg.src = src;
        document.body.style.overflow = "hidden"; // Prevent scrolling
    }
}

// Close Image Modal
function closeImageModal() {
    const modal = document.getElementById('imageModal');
    if (modal) {
        modal.style.display = "none";
        document.body.style.overflow = "auto"; // Restore scrolling
    }
}

// Close location details and cleanup
function closeLocationDetails() {
    const detailsSection = document.getElementById('locationDetailsSection');
    detailsSection.style.display = 'none';

    // Remove show-details class
    document.querySelector('.sidebar').classList.remove('show-details');

    // Hide floor markers
    hideFloorMarkers();

    // Show campus markers again
    if (window.markersLayer && !map.hasLayer(window.markersLayer)) {
        map.addLayer(window.markersLayer);
    }

    // Reset selected university highlighting
    if (currentSelectedUniversity && universityLayer) {
        universityLayer.eachLayer(function (layer) {
            const name = layer.feature.properties.Names;
            if (name === currentSelectedUniversity) {
                layer.setStyle({
                    weight: 3,
                    opacity: 0.8,
                    color: '#000000'
                });
            }
        });
    }

    currentSelectedUniversity = null;
    currentSelectedFloor = null;
}

// Make functions globally accessible
window.closeLocationDetails = closeLocationDetails;
window.selectUniversityFloor = selectUniversityFloor;
window.switchUniversityTab = switchUniversityTab;
window.showUniversitySidebarDetails = showUniversitySidebarDetails;

// Call initialization when map is ready
// This will be called from map.js after the map is created
console.log('🎓 University polygon system loaded and ready');
