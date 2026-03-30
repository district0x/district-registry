# Council UI Integration Guide

**Date:** January 21, 2026
**Status:** Council page and contract helpers created, ready for integration
**Created by:** Ralph

---

## Files Created

### 1. Council Page Component
**Location:** `src/district_registry/ui/council/page.cljs`

Features:
- 9-seat council grid visualization
- Individual council seat cards showing:
  - Seat index and name
  - Total staked DNT
  - Stake percentage (weight)
  - Immutable badge for Seat 8 (ARI/The Stranger)
- GraphQL integration for fetching council data
- User stakes panel placeholder
- "How It Works" information section
- Loading spinner with matching design

### 2. Council Contract Helpers
**Location:** `src/district_registry/ui/contract/council.cljs`

Events:
- `::stake-on-seat` - Stake DNT on a council seat
- `::unstake-from-seat` - Unstake DNT from a council seat
- `::move-stake` - Move stake atomically between two seats
- `::approve-dnt` - Approve DNT for CouncilStakeBank to spend

Subscriptions:
- `::seat-stake-for-user` - Get user's stake for specific seat (placeholder)
- `::user-total-staked` - Get user's total stake across all seats (placeholder)

---

## Required Integrations

### Step 1: Update routes.cljs

**File:** `src/district_registry/shared/routes.cljs`

Change:
```clojure
(def routes [["/" :route/home]
             ["/about" :route/about]
             ["/submit" :route/submit]
             ["/detail/:address" :route/detail]
             ["/edit/:address" :route/edit]
             ["/my-account/:tab" :route/my-account]
             ["/privacy-policy" :route/privacy-policy]
             ["/terms" :route/terms]])
```

To:
```clojure
(def routes [["/" :route/home]
             ["/about" :route/about]
             ["/council" :route/council]             ;; ADD THIS LINE
             ["/submit" :route/submit]
             ["/detail/:address" :route/detail]
             ["/edit/:address" :route/edit]
             ["/my-account/:tab" :route/my-account]
             ["/privacy-policy" :route/privacy-policy]
             ["/terms" :route/terms]])
```

---

### Step 2: Update core.cljs

**File:** `src/district_registry/ui/core.cljs`

Add require for council page (around line 10-20):

```clojure
(ns district-registry.ui.core
  (:require
    ;; ... existing requires
    [district-registry.ui.about.page]
    [district-registry.ui.council.page]          ;; ADD THIS LINE
    [district-registry.ui.detail.page]
    ;; ... rest of requires
    ))
```

---

### Step 3: Update app-layout.cljs Navigation

**File:** `src/district_registry/ui/components/app_layout.cljs`

Add Council to the navigation (around line 13-29):

```clojure
(defn header [active-page-name]
  [:header#globalHeader
   [:div.container
    (nav/div {:class "logo sized"
              :route [:route/home]}
      [:img {:src "/images/registry-logo@2x.png"}])
    [:nav.toplinks
     [:ul
      [:li {:class (when (= active-page-name :route/council)    ;; ADD THIS BLOCK
                     "on")}
       [nav/a {:route [:route/council]}
        "Council"]]
      [:li {:class (when (= active-page-name :route/submit)
                     "on")}
       [nav/a {:route [:route/submit]}
        "Submit"]]
      [:li
       {:class (when (= active-page-name :route/about)
                 "on")}
       [nav/a {:route [:route/about]}
        "About"]]]]
    ;; ... rest of header
    ]])
```

Also update the page ID mapping (around line 93-102):

```clojure
(defn app-layout []
  (let [active-page (subscribe [::router-subs/active-page])]
    (fn [& children]
      [:div {:id (case (:name @active-page)
                   :route/about "page-about"
                   :route/council "page-council"          ;; ADD THIS LINE
                   :route/detail "page-details"
                   :route/home "page-registry"
                   :route/submit "page-submit"
                   :route/edit "page-submit"
                   :route/my-account "page-my-account"
                   :route/privacy-policy "page-privacy-policy"
                   :route/terms "page-terms"
                   :route/not-found "not-found")}
       ;; ... rest of layout
       ])))
```

---

### Step 4: Add CSS Styling

**Create:** `resources/public/css/council.css` or add to existing CSS

```css
/* Council Page Styles */

#page-council {
  background: #f5f5f5;
}

#council-intro {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 60px 0;
  text-align: center;
}

#council-intro h1 {
  font-size: 3rem;
  margin-bottom: 20px;
}

#council-intro .council-description {
  font-size: 1.2rem;
  max-width: 800px;
  margin: 0 auto;
  line-height: 1.6;
}

/* Council Grid */

.council-grid-layout {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
  margin: 40px 0;
}

@media (max-width: 968px) {
  .council-grid-layout {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .council-grid-layout {
    grid-template-columns: 1fr;
  }
}

/* Council Seat Card */

.council-seat-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.council-seat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.council-seat-card.stranger-seat {
  border: 3px solid #f59e0b;
  background: linear-gradient(135deg, #fff9e6 0%, #ffffff 100%);
}

.seat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.seat-index {
  font-size: 0.875rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.immutable-badge {
  background: #f59e0b;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.seat-body {
  padding: 24px 20px;
}

.seat-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 20px;
}

.seat-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

.stat-value {
  font-size: 1rem;
  color: #1f2937;
  font-weight: 700;
}

.seat-footer {
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}

.stake-btn {
  width: 100%;
  padding: 12px 24px;
  font-size: 1rem;
  font-weight: 600;
}

/* User Stakes Panel */

.user-stakes-panel {
  background: white;
  border-radius: 12px;
  padding: 30px;
  margin: 40px 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.user-stakes-panel h3 {
  font-size: 1.5rem;
  margin-bottom: 10px;
  color: #1f2937;
}

.user-stakes-list {
  margin-top: 20px;
}

.coming-soon {
  color: #6b7280;
  font-style: italic;
}

/* Info Section */

#council-info {
  background: white;
  padding: 60px 0;
  margin-top: 60px;
}

#council-info h2 {
  text-align: center;
  font-size: 2.5rem;
  margin-bottom: 40px;
  color: #1f2937;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 30px;
  margin-top: 40px;
}

.info-card {
  padding: 30px;
  background: #f9fafb;
  border-radius: 12px;
  border-left: 4px solid #667eea;
}

.info-card h3 {
  font-size: 1.25rem;
  color: #667eea;
  margin-bottom: 12px;
}

.info-card p {
  color: #4b5563;
  line-height: 1.6;
}

/* Error States */

.no-seats,
.error-message {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
}

.error-message {
  color: #ef4444;
}
```

---

### Step 5: Compile and Test

```bash
# Compile ClojureScript
bb compile-ui

# Or if using shadow-cljs
npx shadow-cljs compile app

# Start the development server
npm run dev
```

---

## GraphQL Query Used

The Council page uses the following GraphQL query:

```graphql
query GetCouncilSeats {
  searchCouncilSeats(
    onlyActive: true
    orderBy: council_seats_order_by_index
    orderDir: asc
    first: 9
  ) {
    totalCount
    items {
      councilSeat_index
      councilSeat_mindId
      councilSeat_name
      councilSeat_philosophyHash
      councilSeat_isImmutable
      councilSeat_isActive
      councilSeat_createdOn
      councilSeat_totalStaked
      councilSeat_stakePercentage
    }
  }
}
```

This query is automatically constructed in the `council-seats-query` definition in `page.cljs`.

---

## Next Steps

### Immediate
1. ✅ Council page component created
2. ✅ Contract helpers created
3. ⏳ Integrate into routing (Step 1-3 above)
4. ⏳ Add CSS styling (Step 4 above)
5. ⏳ Test Council page loads correctly

### Short-term
1. Create StakeModal component for staking/unstaking
2. Add user's personal stake display (query from GraphQL)
3. Implement move-stake functionality
4. Add philosophy display (fetch from IPFS)

### Medium-term
1. Create ARI Activity page (`/ari`)
2. Create Power Plant page (`/power-plant`)
3. Add proposal veto interface
4. Add bounty/grant displays

---

## Testing Checklist

- [ ] Council route navigates to `/council`
- [ ] 9 council seats display in grid
- [ ] Seat 8 shows "IMMUTABLE" badge
- [ ] Seat names display correctly
- [ ] Stake percentages display correctly
- [ ] Total staked amounts display
- [ ] Stake button is clickable
- [ ] Loading spinner shows while fetching
- [ ] Error message shows if GraphQL fails
- [ ] Responsive design works on mobile

---

## Known Limitations

1. **Stake Modal**: Not yet implemented - clicking "Stake on this Mind" button will not do anything yet
2. **User Stakes**: User-specific stake queries not yet implemented (placeholder shows "coming soon")
3. **Philosophy Display**: IPFS fetch for philosophy text not yet implemented
4. **DNT Approval**: Two-step process (approve + stake) needs UI flow
5. **Move Stake**: Feature exists in contract helpers but no UI component yet

---

## Contract Addresses Required

The Council UI expects these smart contracts to be deployed and available in the smart contracts registry:

- `:council` - Council.sol
- `:council-stake-bank` - CouncilStakeBank.sol
- `:dnt` - District0xNetworkToken (MiniMeToken)

Ensure the migration script has run successfully and contracts are registered in `smart_contracts_dev.cljs` or `smart_contracts_prod.cljs`.

---

## Questions or Issues?

See the following files for reference:
- `BACKROOM_IMPLEMENTATION_STATUS.md` - Full backend status
- `@fix_plan.md` - Implementation roadmap
- `THE_BACKROOM.md` - Vision and philosophy
- `RALPH_FINAL_SUMMARY.md` - Complete implementation details

---

**Status:** Ready for integration - Just add 3 small edits to existing files!
**Next:** Follow Steps 1-5 above to integrate Council page into the app.
