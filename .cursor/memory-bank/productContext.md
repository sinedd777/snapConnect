# Product Context

## Problem Statement
College students want to discover and join local events and gatherings in their campus area, share authentic moments with others attending these events, and create lasting memories while maintaining control over their digital footprint.

## Solution
SnapCircle provides a map-based, ephemeral sharing platform specifically designed for college communities that:
- Shows nearby events and gatherings as Circles on a map interface
- Enables creation of public or private Circles tied to specific locations
- Supports collaborative storytelling through shared photos, videos, text, and reactions
- Ensures content auto-deletion after Circle expiration
- Maintains privacy through Circle-only sharing
- Offers engaging AR filters and effects
- Leverages Retrieval-Augmented Generation (RAG) for personalized, context-aware features
- Creates AI-generated summaries of expired Circles

## Current Implementation Focus
- Authentication via email/password ✅ (Pivoting to OAuth)
- Basic camera functionality for photo capture ✅
- Initial snap storage and metadata ✅
- Simple navigation between auth, home, and camera screens ✅
- Friend management system ✅
- Snap sharing workflow ✅
- Basic AR filter UI ✅
- Map-based interface (in planning)
- Circle creation and management (in planning)
- College town selection (in planning)
- RAG integration (in planning)

## User Experience Goals

### New User Experience
1. Simple OAuth registration ⭕ (Pending)
2. College town selection ⭕ (Pending)
3. Map-based Circle discovery ⭕ (Pending)
4. Clear privacy controls ⭕ (Pending)

### Map Experience
1. Location-centered map view ⭕ (Pending)
2. Circle pins with size based on participant count ⭕ (Pending)
3. Filter options (Active, Upcoming, Public, Private, Category) ⭕ (Pending)
4. Smooth scrolling and zooming ⭕ (Pending)
5. Circle preview on tap ⭕ (Pending)

### Circle Management
1. Easy Circle creation with map pin placement ⭕ (Pending)
2. Flexible duration settings (1 hour to 7 days) ⭕ (Pending)
3. Public or private visibility options ⭕ (Pending)
4. Simple invite system with shareable codes ⭕ (Pending)
5. AR filter selection for Circle theme ⭕ (Pending)

### Content Creation
1. Instant camera access ✅
2. Real-time AR filters 🟡 (Basic UI implemented)
3. Video recording (≤30 secs) ⭕ (Pending)
4. Text posts (≤280 chars) ⭕ (Pending)
5. Collaborative story contributions ⭕ (Pending)
6. Live reactions to shared content ⭕ (Pending)
7. RAG-enhanced caption and filter suggestions ⭕ (Pending)

### Content Consumption
1. Circle story feed ⭕ (Pending)
2. Reaction options (emoji and video) ⭕ (Pending)
3. Group chat within Circles ⭕ (Pending)
4. AI-generated Circle summaries ⭕ (Pending)
5. Memory Vault for saved content ⭕ (Pending)

### Event Discovery
1. RAG-powered event recommendations ⭕ (Pending)
2. Integration with public event sources ⭕ (Pending)
3. Trending Circles in college town ⭕ (Pending)

## User Flows

### Set Up Account
1. Open app on iOS/Android ⭕ (Pending)
2. Select OAuth provider (Google, Apple, phone) ⭕ (Pending)
3. Verify via code/link ⭕ (Pending)
4. Set username and optional profile picture ⭕ (Pending)
5. Select college town ⭕ (Pending)
6. Agree to terms and privacy policy ⭕ (Pending)
7. Land on map-based home screen ⭕ (Pending)

### Discover Circles on Map
1. View map centered on user's location (1 km radius) ⭕ (Pending)
2. Apply filters (Active, Upcoming, Public, Private, Category) ⭕ (Pending)
3. Scroll/zoom map to see Circle pins ⭕ (Pending)
4. Tap Circle pin to view details ⭕ (Pending)
5. Request to join Circle ⭕ (Pending)

### Create a Circle
1. Tap "Create Circle" button ⭕ (Pending)
2. Enter Circle details (title, description, location, duration, start time) ⭕ (Pending)
3. Choose public or private visibility ⭕ (Pending)
4. Select AR filter theme ⭕ (Pending)
5. Send invites or generate shareable code ⭕ (Pending)

### Share Snaps in a Circle
1. Open joined Circle ⭕ (Pending)
2. Add photo, video, or text ⭕ (Pending)
3. Apply AR filter or select AI-suggested caption ⭕ (Pending)
4. Post to Circle's shared story ⭕ (Pending)

### React to Snaps
1. View snaps in Circle's story feed ⭕ (Pending)
2. Add emoji or video reaction ⭕ (Pending)
3. See reactions as overlays ⭕ (Pending)
4. Receive notification for reactions to your content ⭕ (Pending)

### Chat in a Circle
1. Open Circle chat tab ⭕ (Pending)
2. Send text, emojis, or GIFs ⭕ (Pending)
3. Use AI-suggested replies or translations ⭕ (Pending)

### Save to Memory Vault
1. Save content before Circle expires ⭕ (Pending)
2. Access encrypted vault from profile ⭕ (Pending)

### View Circle Summary Post-Expiration
1. Receive notification when Circle expires ⭕ (Pending)
2. View AI-generated summary ⭕ (Pending)
3. Share or save summary ⭕ (Pending)

## RAG-Enhanced Features (Planned)
1. AI-Generated Circle Captions and Suggestions
2. Smart Event Summaries
3. Contextual Safety & Privacy Guidance
4. Personalized Event Recommendations
5. Real-Time Trending Analysis
6. Multicultural Support

## Success Metrics
1. Engagement: 50% of users create/join at least one Circle per week
2. Retention: 70% monthly active users after 3 months
3. Content Creation: Average 5 contributions per user per Circle
4. RAG Relevance: 90% of AI suggestions rated relevant by users
5. Privacy Satisfaction: 95% user satisfaction with privacy controls
6. Growth: 100K downloads within 6 months of launch
7. Geographic Diversity: Active in 50+ college towns within first year 