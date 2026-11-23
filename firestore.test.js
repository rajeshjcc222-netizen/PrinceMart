const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const STUDENT_UID = "student_rahul_123";
const OTHER_STUDENT_UID = "student_amit_999";
const SHOP_OWNER_UID = "shop_sharma_456";
const OTHER_SHOP_UID = "shop_verma_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp(msAgo = 2000) {
  return new Date(Date.now() - msAgo);
}

function futureTimestamp(msAhead = 60000) {
  return new Date(Date.now() + msAhead);
}

function validStudentPayload(uid = STUDENT_UID) {
  const ts = pastTimestamp(5000);
  return {
    userId: uid,
    name: "Rahul Kumar",
    mobile: "9876543210",
    hostelName: "Tagore Bhawan",
    roomNumber: "204",
    deliveryPreference: "HOSTEL_GATE",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validShopPayload(ownerId = SHOP_OWNER_UID, name = "Sharma Kirana Store") {
  const ts = pastTimestamp(5000);
  return {
    ownerId,
    shopName: name,
    category: "Kirana & General",
    mobile: "9123456789",
    distance: "60m",
    shopOpen: true,
    listed: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCommonOrderPayload(studentUid = STUDENT_UID) {
  const ts = pastTimestamp(5000);
  return {
    studentUid,
    studentName: "Rahul Kumar",
    studentMobile: "9876543210",
    hostelName: "Tagore Bhawan",
    roomNumber: "204",
    orderMode: "COMMON",
    broadcastOpen: true,
    deliveryMode: "HOSTEL_GATE",
    paymentMethod: "UPI_GATE",
    pickupPin: "4829",
    itemsSummary: ["Maggi Masala x2 (₹28)", "Adrak Chai x1 (₹15)"],
    totalAmount: 43,
    customNote: "Thoda jaldi bhej dena bhaiya",
    voiceNoteBase64: "",
    voiceNoteDurationSec: 0,
    targetShopId: "",
    targetShopName: "",
    targetShopOwnerId: "",
    status: "PENDING",
    acceptedShopId: "",
    acceptedShopName: "",
    acceptedShopMobile: "",
    acceptedShopOwnerId: "",
    acceptedShopDistance: "",
    skippedShopIds: [],
    createdAt: ts,
    updatedAt: ts,
  };
}

function validDirectOrderPayload(shopId, shopOwnerId = SHOP_OWNER_UID, studentUid = STUDENT_UID) {
  return {
    ...validCommonOrderPayload(studentUid),
    orderMode: "DIRECT",
    broadcastOpen: false,
    targetShopId: shopId,
    targetShopName: "Sharma Kirana Store",
    targetShopOwnerId: shopOwnerId,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated bounds
test("Unauthenticated user cannot read or write students, shops, or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("students").doc(STUDENT_UID).get());
  await assertFails(unauthDb.collection("students").doc(STUDENT_UID).set(validStudentPayload()));
  await assertFails(unauthDb.collection("shops").where("listed", "==", true).get());
  await assertFails(unauthDb.collection("orders").where("broadcastOpen", "==", true).get());
});

// 2. Student Profile ownership & PII isolation
test("Student can create/read/update own profile; other user cannot read or overwrite", async () => {
  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_STUDENT_UID).firestore();

  await assertSucceeds(studentDb.collection("students").doc(STUDENT_UID).set(validStudentPayload()));
  await assertSucceeds(studentDb.collection("students").doc(STUDENT_UID).get());

  // Valid partial update
  await assertSucceeds(
    studentDb.collection("students").doc(STUDENT_UID).update({
      roomNumber: "305",
      deliveryPreference: "SHOP_PICKUP",
      updatedAt: pastTimestamp(1000),
    })
  );

  // Reject future timestamp & ghost field
  await assertFails(
    studentDb.collection("students").doc(STUDENT_UID).update({
      roomNumber: "306",
      updatedAt: futureTimestamp(),
    })
  );
  await assertFails(
    studentDb.collection("students").doc(STUDENT_UID).update({
      isAdmin: true,
      updatedAt: pastTimestamp(500),
    })
  );

  // Cross-user PII isolation
  await assertFails(otherDb.collection("students").doc(STUDENT_UID).get());
  await assertFails(
    otherDb.collection("students").doc(STUDENT_UID).update({
      name: "Hijacked",
      updatedAt: pastTimestamp(500),
    })
  );
  await assertFails(otherDb.collection("students").get());
});

// 3. Shops registration, listing, and owner-only toggle/update
test("Shopkeeper registers shop, toggles open/closed; non-owner can list listed shops but cannot modify", async () => {
  const shopDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  const otherShopDb = testEnv.authenticatedContext(OTHER_SHOP_UID).firestore();

  await assertSucceeds(shopDb.collection("shops").doc("shop_1").set(validShopPayload()));

  // Extra @DocumentId 'id' field rejected
  await assertFails(
    shopDb.collection("shops").doc("shop_bad").set({
      ...validShopPayload(),
      id: "shop_bad",
    })
  );

  // Student filtered query succeeds; unfiltered query fails
  await assertSucceeds(studentDb.collection("shops").where("listed", "==", true).get());
  await assertFails(studentDb.collection("shops").get());

  // Owner queries own shops & toggles shopOpen
  await assertSucceeds(shopDb.collection("shops").where("ownerId", "==", SHOP_OWNER_UID).get());
  await assertSucceeds(
    shopDb.collection("shops").doc("shop_1").update({
      shopOpen: false,
      updatedAt: pastTimestamp(1000),
    })
  );

  // Another user cannot toggle or delete someone else's shop
  await assertFails(
    otherShopDb.collection("shops").doc("shop_1").update({
      shopOpen: true,
      updatedAt: pastTimestamp(500),
    })
  );
  await assertFails(otherShopDb.collection("shops").doc("shop_1").delete());
});

// 4. Common broadcast order lifecycle: Create -> Skip -> Accept -> Packing -> At Gate -> Delivered -> Terminal Lock
test("Common order full lifecycle and state-machine enforcement", async () => {
  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  const shopDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const otherShopDb = testEnv.authenticatedContext(OTHER_SHOP_UID).firestore();

  // Register two shops
  await assertSucceeds(shopDb.collection("shops").doc("shop_1").set(validShopPayload(SHOP_OWNER_UID, "Sharma Kirana")));
  await assertSucceeds(otherShopDb.collection("shops").doc("shop_2").set(validShopPayload(OTHER_SHOP_UID, "Verma Canteen")));

  // Student places COMMON broadcast order
  await assertSucceeds(studentDb.collection("orders").doc("order_1").set(validCommonOrderPayload()));

  // Student queries own orders; shopkeeper queries open broadcasts; unfiltered query fails
  await assertSucceeds(studentDb.collection("orders").where("studentUid", "==", STUDENT_UID).get());
  await assertSucceeds(shopDb.collection("orders").where("broadcastOpen", "==", true).get());
  await assertFails(studentDb.collection("orders").get());

  // Shop 2 skips the common order
  await assertSucceeds(
    otherShopDb.collection("orders").doc("order_1").update({
      skippedShopIds: ["shop_2"],
      updatedAt: pastTimestamp(1500),
    })
  );

  // Shop 1 accepts the common order
  await assertSucceeds(
    shopDb.collection("orders").doc("order_1").update({
      status: "ACCEPTED",
      broadcastOpen: false,
      acceptedShopId: "shop_1",
      acceptedShopName: "Sharma Kirana",
      acceptedShopMobile: "9123456789",
      acceptedShopOwnerId: SHOP_OWNER_UID,
      acceptedShopDistance: "60m",
      updatedAt: pastTimestamp(1200),
    })
  );

  // Another shopkeeper cannot read or steal an already accepted order
  await assertFails(otherShopDb.collection("orders").doc("order_1").get());
  await assertFails(
    otherShopDb.collection("orders").doc("order_1").update({
      status: "PACKING",
      updatedAt: pastTimestamp(1000),
    })
  );

  // State shortcutting (ACCEPTED -> DELIVERED directly) is rejected!
  await assertFails(
    shopDb.collection("orders").doc("order_1").update({
      status: "DELIVERED",
      updatedAt: pastTimestamp(1000),
    })
  );

  // Step-by-step progression succeeds: ACCEPTED -> PACKING -> AT_GATE -> DELIVERED
  await assertSucceeds(
    shopDb.collection("orders").doc("order_1").update({
      status: "PACKING",
      updatedAt: pastTimestamp(900),
    })
  );
  await assertSucceeds(
    shopDb.collection("orders").doc("order_1").update({
      status: "AT_GATE",
      updatedAt: pastTimestamp(800),
    })
  );
  await assertSucceeds(
    shopDb.collection("orders").doc("order_1").update({
      status: "DELIVERED",
      updatedAt: pastTimestamp(700),
    })
  );

  // Terminal state locking: no further updates allowed after DELIVERED
  await assertFails(
    shopDb.collection("orders").doc("order_1").update({
      status: "PACKING",
      updatedAt: pastTimestamp(500),
    })
  );
});

// 5. Direct order targeting, decline, and student cancellation
test("Direct order targeting enforces valid shop ownership and allows target decline or student cancel", async () => {
  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  const shopDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const otherShopDb = testEnv.authenticatedContext(OTHER_SHOP_UID).firestore();

  await assertSucceeds(shopDb.collection("shops").doc("shop_1").set(validShopPayload(SHOP_OWNER_UID, "Sharma Kirana")));
  await assertSucceeds(otherShopDb.collection("shops").doc("shop_2").set(validShopPayload(OTHER_SHOP_UID, "Verma Canteen")));

  // Spoofed targetShopOwnerId rejected
  await assertFails(
    studentDb.collection("orders").doc("order_bad_direct").set(
      validDirectOrderPayload("shop_1", OTHER_SHOP_UID)
    )
  );

  // Valid direct order succeeds
  await assertSucceeds(
    studentDb.collection("orders").doc("order_direct_1").set(
      validDirectOrderPayload("shop_1", SHOP_OWNER_UID)
    )
  );

  // Target shopkeeper can query their direct orders; non-target shop cannot read or accept it
  await assertSucceeds(shopDb.collection("orders").where("targetShopOwnerId", "==", SHOP_OWNER_UID).get());
  await assertFails(otherShopDb.collection("orders").doc("order_direct_1").get());
  await assertFails(
    otherShopDb.collection("orders").doc("order_direct_1").update({
      status: "ACCEPTED",
      broadcastOpen: false,
      acceptedShopId: "shop_2",
      acceptedShopName: "Verma Canteen",
      acceptedShopMobile: "9123456789",
      acceptedShopOwnerId: OTHER_SHOP_UID,
      acceptedShopDistance: "60m",
      updatedAt: pastTimestamp(1000),
    })
  );

  // Target shopkeeper can decline direct order
  await assertSucceeds(
    shopDb.collection("orders").doc("order_direct_1").update({
      status: "DECLINED",
      broadcastOpen: false,
      updatedAt: pastTimestamp(900),
    })
  );

  // Student can cancel their own pending order
  await assertSucceeds(
    studentDb.collection("orders").doc("order_cancel_1").set(validCommonOrderPayload())
  );
  await assertSucceeds(
    studentDb.collection("orders").doc("order_cancel_1").update({
      status: "CANCELLED",
      broadcastOpen: false,
      updatedAt: pastTimestamp(800),
    })
  );
});
