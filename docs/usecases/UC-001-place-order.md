# UC-001 — Submit an Order Request (Lesson 1.1.2)

**Actor:** Customer (identity supplied in request, **not authenticated** in this baseline).  
**Stakeholders:** Restaurant owns fulfillment; external provider owns financial results; platform operations owns intake communication.  
**Success criterion:** A valid request is recorded in *volatile memory* with a stable order-request identity. That does **not** mean restaurant acceptance or payment success.

## Trigger / preconditions

Customer sends POST /api/orders with UUID customerId, known fixture restaurantId and basket of 1–20 **distinct** menu codes, each quantity 1–20. The server's synthetic restaurant menu defines available items and quoted unit prices. This is a learning fixture, not a live menu or binding commercial offer.

## Normal flow

1. HTTP adapter validates the shape of input, without claiming authentic caller identity.
2. Application use case locates a restaurant's fixture menu and checks each requested item, availability and quantity.
3. Domain order-request record is created; total is calculated from server-owned item prices.
4. In-memory adapter stores the new resource for this process.
5. HTTP response is **201 Created**, with Location header and `intakeStatus: REQUESTED_AWAITING_RESTAURANT`, `restaurantDecision: PENDING`, `paymentStatus: NOT_STARTED`.
6. GET on the Location returns recorded intake while this process remains alive.

## Alternate/rejection flows

- Malformed JSON, missing/invalid UUID, invalid item quantities, empty basket → HTTP 400 INVALID_REQUEST.
- Unknown fixture restaurant → HTTP 422 RESTAURANT_NOT_FOUND.
- Item absent from selected menu → HTTP 422 ITEM_NOT_FOUND.
- Unavailable fixture item → HTTP 422 ITEM_UNAVAILABLE.
- Duplicate item lines → HTTP 422 DUPLICATE_ITEM.
- GET missing order → HTTP 404.
- Store unavailable/unexpected failure → not modeled as a successful business outcome; 5xx possible, no durability promise.
- Merchant rejects later, payment declines, customer cancels during preparation, refund requested → **scenario analysis only; not implemented endpoints**.

## Evidence, counterexample and limits

A 201 confirms *only* platform intake resource creation. If the restaurant never accepts, the 201 is not incorrect provided its wording accurately communicates pending merchant decision. The platform must not show paid/accepted. Repeating POST **is not idempotent** and may create another record. Demo prices are untrusted as commercial quotes and currency is set to USD solely for the synthetic fixture; no settlement occurs.

## Acceptance criteria mapping

| ID | Given / When / Then | Evidence |
|---|---|---|
| AC-01 | Valid known available menu item → 201 + Location, pending merchant and no payment | OrderControllerIntegrationTest |
| AC-02 | Intake read by returned Location → 200 and same status | OrderControllerIntegrationTest |
| AC-03 | Invalid shape or invalid quantity → 400 | OrderControllerIntegrationTest |
| AC-04 | Unavailable or unknown fixture item/restaurant → 422, no successful intake response | ServiceTest / ControllerIntegrationTest |
| AC-05 | Repeated POST → distinct order IDs (explicit non-idempotency) | PlaceOrderServiceTest |
| AC-06 | Quoted total derives from server fixture unit price, not client price | PlaceOrderServiceTest |
| AC-07 | Unknown order read → 404 | OrderControllerIntegrationTest |

## Assumptions requiring business decision

Merchant acceptance mechanism, out-of-stock updates, concurrent availability, cancellation policy, price validity/freshness and tax, card authorization/capture/settlement, customer authentication, privacy and fraud controls, local and distributed consistency. All deferred; do not claim endpoint exists for these.
