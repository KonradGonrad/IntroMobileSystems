# DormSwap

**DormSwap** is a small mobile application designed to address a clearly defined social 
and environmental problem: the waste of usable goods in student dormitories.

## M1: Problem and Prototype

### The Problem
Students living in dormitories or close to campus constantly need to acquire or get rid of 
everyday items like textbooks, kitchenware, small furniture, and electronics. The demand 
peaks heavily at the start and end of each semester, especially during move-outs for 
international students.

As a result, perfectly usable items often end up in the trash at move-out, while new students 
spend money on things they could easily get for free or very cheaply from someone living 
in the exact same building.

### Why Existing Options Fail

Current solutions do not solve this problem efficiently:

* **General marketplaces (e.g., OLX):** Built for shipping and dealing with random users across the whole country. They are cluttered with commercial listings and lack the "local community" feel.
* **Dormitory Facebook groups:** Extremely chaotic. Posts get buried quickly, there is no proper search or filtering, and not every student uses Facebook (which is especially true for international students).

### The Solution
A dedicated, local mobile application where students can list and claim items within their specific dormitory.

---

## Scope and Target
- **Target User Group:** University students living in dormitories.
- **Core User Stories:**
    1. As a student, I want to browse available items in my dorm so I can find things I need cheaply.
    2. As a student moving out, I want to quickly list my items so they don't end up in the trash.
    3. As a user without internet access in a specific dorm area, I want to see previously loaded items to check their details.

## Architecture & Scope (3 Main Screens)
The app is built using MVVM architecture and focuses on local storage (offline-first approach using Room) without a complex remote backend or authentication, to keep the flow simple and efficient.

1. **Feed Screen:** A dashboard displaying available items with basic filtering.

  ![Feed Screen](./ui-prototype/png/DormSwap_FeedScreen.png)

2. **Item Detail Screen:** Shows the full photo, description, condition, and a "Reserve/Contact" action button.

  ![Item Detail Screen](./ui-prototype/png/DormSwap_DetailsScreen.png)

3. **Add Item Screen:** A simple form to list a new item (title, photo, category, description).

  ![Add Listing Scree](./ui-prototype/png/DormSwap_AddListingScreen.png)

## User Flow
1. **Browse:** User opens the app and sees the local `Feed Screen`.
2. **Inspect:** User taps on an interesting item and navigates to the `Item Detail Screen` to read more.
3. **Contribute:** User taps the floating action button (+) on the Feed to open the `Add Item Screen`, fills out the form, and publishes a new listing.

## Prototype
*The UI prototypes (Figma screens) for this milestone are located in the `ui-prototype/`folder and also shown in the **Architecture and Scope** section.*