# The Backroom - Next Steps: UI Integration

**Date:** January 21, 2026
**Status:** Ready for UI Integration
**Priority:** HIGH (Quick Win - 15 minutes)

---

## 🎯 Current Status Summary

### ✅ COMPLETE (100%)
1. **Smart Contracts** - All 4 contracts (Council, CouncilStakeBank, ARIOracle, PowerPlant) - 1,397 lines
2. **Contract Helpers** - 4 helper files for tests - 235 lines
3. **Test Suite** - 75 comprehensive test cases - 1,175 lines
4. **Backend Infrastructure** - Database, event syncer, GraphQL, ARI API - 100% complete
5. **Council UI Page** - Component created at `src/district_registry/ui/council/page.cljs` - 208 lines

### ⏳ READY TO INTEGRATE (15 minutes)
**Council UI Integration** - 3 simple file edits to wire up the Council page

### 📋 PENDING (Future Work)
- Test execution on local testnet
- ARI Activity page
- Power Plant page
- Stake Modal component

---

## 🚀 Immediate Next Step: Integrate Council UI

The Council page is **fully built and ready** - it just needs to be wired into the app routing.

### Required Changes (3 files, ~10 lines total)

#### Edit #1: Add Council Route
**File:** `src/district_registry/shared/routes.cljs`

**Current code:**
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

**Change to:**
```clojure
(def routes [["/" :route/home]
             ["/about" :route/about]
             ["/council" :route/council]           ;; ← ADD THIS LINE
             ["/submit" :route/submit]
             ["/detail/:address" :route/detail]
             ["/edit/:address" :route/edit]
             ["/my-account/:tab" :route/my-account]
             ["/privacy-policy" :route/privacy-policy]
             ["/terms" :route/terms]])
```

---

#### Edit #2: Require Council Page Namespace
**File:** `src/district_registry/ui/core.cljs`

Find the namespace requires section (around line 10-30) and add:

```clojure
[district-registry.ui.council.page]           ;; ← ADD THIS LINE
```

The requires section should look like:
```clojure
(ns district-registry.ui.core
  (:require
    ;; ... existing requires
    [district-registry.ui.about.page]
    [district-registry.ui.council.page]        ;; ← ADD THIS LINE
    [district-registry.ui.detail.page]
    ;; ... rest of requires
    ))
```

---

#### Edit #3: Add Council Navigation and Page ID
**File:** `src/district_registry/ui/components/app_layout.cljs`

**Change 3a:** Add navigation link in header

Find the nav section (around line 20-30) and add:

```clojure
[:nav.toplinks
 [:ul
  [:li {:class (when (= active-page-name :route/council)     ;; ← ADD THIS
                 "on")}                                       ;; ← BLOCK
   [nav/a {:route [:route/council]}                          ;; ←
    "Council"]]                                               ;; ←
  [:li {:class (when (= active-page-name :route/submit)
                 "on")}
   [nav/a {:route [:route/submit]}
    "Submit"]]
  ;; ... rest of nav items
  ]]
```

**Change 3b:** Add page ID mapping

Find the page ID mapping in the app-layout function (around line 90-100):

```clojure
(defn app-layout []
  (let [active-page (subscribe [::router-subs/active-page])]
    (fn [& children]
      [:div {:id (case (:name @active-page)
                   :route/about "page-about"
                   :route/council "page-council"          ;; ← ADD THIS LINE
                   :route/detail "page-details"
                   ;; ... rest of page IDs
                   )}
       ;; ... rest of layout
       ])))
```

---

## 📋 Verification Checklist

After making these 3 edits:

1. **Compile ClojureScript:**
   ```bash
   bb compile-ui
   # Or if using shadow-cljs:
   npx shadow-cljs compile app
   ```

2. **Start development server:**
   ```bash
   npm run dev
   ```

3. **Navigate to Council page:**
   ```
   http://localhost:6400/council
   ```

4. **Verify:**
   - [ ] Route navigates successfully
   - [ ] Page loads without errors
   - [ ] 9 council seats display in grid
   - [ ] Seat 8 shows "IMMUTABLE" badge
   - [ ] Loading spinner works
   - [ ] GraphQL query succeeds (check browser console)
   - [ ] Navigation link highlights when on Council page

---

## 🎨 Optional: Add CSS Styling

The Council page will work with default styles, but for the full visual design, add the CSS from `COUNCIL_UI_INTEGRATION.md`.

**Create:** `resources/public/css/council.css`

Or add to existing main CSS file. The CSS includes:
- Council grid layout (responsive 3→2→1 columns)
- Seat card styling with hover effects
- Stranger seat special styling (gold border)
- Loading states
- Info section styling

See `COUNCIL_UI_INTEGRATION.md` (lines 150-400) for complete CSS.

---

## 🔍 What the Council Page Shows

Once integrated, users will see:

1. **Hero Section** - "The Council" heading with description
2. **9 Council Seats** - Grid layout showing:
   - Seat index (0-8)
   - Philosophical mind name
   - Total DNT staked
   - Stake percentage (council weight)
   - "IMMUTABLE" badge for Seat 8 (ARI/The Stranger)
3. **User Stakes Panel** - Placeholder for user's stakes
4. **How It Works** - Educational section explaining council mechanics

---

## 🚧 Known Limitations (Future Work)

The Council page is read-only visualization. Future enhancements:

1. **Stake Modal** - Allow users to stake DNT on seats
2. **Unstake Functionality** - Withdraw stakes
3. **Move Stake** - Transfer stake between seats (for veto)
4. **Philosophy Display** - Fetch and show IPFS content
5. **User Balance Display** - Show user's DNT balance
6. **Real-time Updates** - WebSocket for live stake changes

---

## 📊 Component Architecture

### Council Page Structure
```
council/page.cljs (208 lines)
├── council-seats-query (GraphQL)
├── loader component
├── council-seat-card component
│   ├── Seat index
│   ├── Mind name
│   ├── Total staked
│   ├── Stake percentage
│   └── Immutable badge
├── user-stakes-panel (placeholder)
├── how-it-works-section
└── page method (routing)
```

### GraphQL Integration
```clojure
[:search-council-seats
 {:only-active true
  :order-by :council-seats.order-by/index
  :order-dir :asc
  :first 9}
 [:total-count
  [:items [/* seat fields */]]]]
```

### Re-frame Subscriptions
```clojure
(subscribe [::gql/query
            {:queries [council-seats-query]}
            {:id :council-page}])
```

---

## 🎯 Why This Is High Priority

1. **Quick Win** - Only 3 small edits, ~15 minutes of work
2. **Visible Progress** - Demonstrates UI implementation to stakeholders
3. **Foundation** - Council page is core to The Backroom vision
4. **No Dependencies** - Doesn't require contracts deployed or tests passing
5. **Low Risk** - Just routing changes, no business logic

---

## 🔄 Alternative: Run Tests First

If you prefer to validate contracts before UI work:

### Test Execution Steps
1. Start ganache: `ganache-cli`
2. Deploy contracts: `npx truffle migrate --reset --network development`
3. Run tests: `npx shadow-cljs compile test`
4. Debug failures and iterate

**Time estimate:** 4-8 hours
**Complexity:** Medium-High
**Blockers:** Requires local blockchain setup

---

## 📖 Related Documentation

- `COUNCIL_UI_INTEGRATION.md` - Full integration guide with CSS
- `HANDOFF_STATUS.md` - Complete project status
- `RALPH_CONTRACT_HELPERS_COMPLETE.md` - Contract helpers status
- `TEST_IMPLEMENTATION_GUIDE.md` - Test execution guide
- `@fix_plan.md` - Implementation roadmap

---

## 💡 Recommendation

**Do the Council UI integration first:**
- Takes only 15 minutes
- Provides immediate visible result
- No dependencies or blockers
- Low risk, high value

**Then proceed to testing:**
- More time-consuming (4-8 hours)
- Higher complexity
- Validates contract correctness
- Required before production

---

## ✅ Success Criteria

After UI integration:
- [ ] `/council` route works
- [ ] Council page loads
- [ ] 9 seats display correctly
- [ ] GraphQL query succeeds
- [ ] No console errors
- [ ] Navigation link works
- [ ] Seat 8 shows immutable badge

---

**Ready to proceed!** The Council page is fully built and just needs these 3 simple routing edits to go live.

---

*Status document created by Ralph on January 21, 2026*
