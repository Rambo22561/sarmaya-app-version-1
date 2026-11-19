const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "admin_uid_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: "khan561salman@gmail.com",
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: "khan561salman@gmail.com",
    email_verified: false,
  });
}

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
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

test("Unauthenticated access is denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("projects").where("published", "==", true).get());
  await assertFails(unauthDb.collection("payment_requests").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("ownerships").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("withdrawals").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("ledger_entries").where("userId", "==", ALICE_UID).get());
});

test("User profile creation enforces INVESTOR role, zero balances, and PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  // Valid INVESTOR creation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Investor",
      email: "alice@example.com",
      phone: "03001234567",
      role: "INVESTOR",
      eligibleMember: false,
      referralCode: "SSALICE1",
      availableBalance: 0,
      pendingBalance: 0,
      investedCapital: 0,
      realizedProfit: 0,
      buybackProceeds: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-assigned ADMIN role or non-zero balance
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Escalator",
      email: "bob@example.com",
      phone: "03009999999",
      role: "ADMIN",
      eligibleMember: true,
      referralCode: "SSBOB999",
      availableBalance: 50000,
      pendingBalance: 0,
      investedCapital: 0,
      realizedProfit: 0,
      buybackProceeds: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject Ghost Field injection (Shadow Update Test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      isVerifiedGhost: true,
      updatedAt: pastDate(1000),
    })
  );

  // Bob cannot read Alice's PII or list /users
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Email spoofing with unverified admin email is rejected", async () => {
  const spoofDb = spoofedAdminContext().firestore();
  const now = pastDate(3000);
  await assertFails(
    spoofDb.collection("projects").doc("proj_spoof").set({
      title: "Fake Project",
      category: "FMCG",
      description: "Spoofed",
      imageKey: "detergent",
      purchaseCost: 10000,
      expectedSellingPrice: 12000,
      totalUnits: 100,
      availableUnits: 100,
      published: true,
      status: "OPEN",
      createdBy: "spoof_uid",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Projects, packs, manual payment verification, and duplicate approval guard", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  // Admin creates published project with imageGallery + pack and updates it
  await assertSucceeds(
    adminDb.collection("projects").doc("proj_1").set({
      title: "Washing Powder Stock",
      category: "Household",
      description: "Bulk washing powder inventory batch",
      imageKey: "detergent",
      imageGallery: ["data:image/jpeg;base64,/9j/4AAQSkZJRg==", "https://example.com/wash.jpg"],
      purchaseCost: 100000,
      expectedSellingPrice: 120000,
      totalUnits: 1000,
      availableUnits: 1000,
      published: true,
      status: "OPEN",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Regular user cannot create or update projects or packs
  await assertFails(
    aliceDb.collection("projects").doc("proj_1").update({
      title: "Hacked",
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    adminDb.collection("projects").doc("proj_1").collection("packs").doc("pack_a").set({
      projectId: "proj_1",
      packName: "Pack A",
      price: 140,
      unitsPerPack: 1,
      totalPacks: 100,
      availablePacks: 100,
      minQty: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds for investor; unfiltered query fails
  await assertSucceeds(aliceDb.collection("projects").where("published", "==", true).get());
  await assertFails(aliceDb.collection("projects").get());

  // Investor cannot create payment request pre-APPROVED
  await assertFails(
    aliceDb.collection("payment_requests").doc("pay_bad").set({
      userId: ALICE_UID,
      userName: "Alice",
      projectId: "proj_1",
      projectTitle: "Washing Powder Stock",
      packId: "pack_a",
      packName: "Pack A",
      quantity: 2,
      unitPrice: 140,
      totalAmount: 280,
      paymentMethod: "EASYPAISA",
      transactionRef: "EP-123456",
      proofSummary: "Screenshot attached",
      status: "APPROVED",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Investor creates PENDING payment request
  await assertSucceeds(
    aliceDb.collection("payment_requests").doc("pay_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      projectId: "proj_1",
      projectTitle: "Washing Powder Stock",
      packId: "pack_a",
      packName: "Pack A",
      quantity: 2,
      unitPrice: 140,
      totalAmount: 280,
      paymentMethod: "EASYPAISA",
      transactionRef: "EP-123456",
      proofSummary: "Screenshot attached",
      status: "PENDING",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's payment request
  await assertFails(bobDb.collection("payment_requests").doc("pay_1").get());
  await assertFails(bobDb.collection("payment_requests").where("userId", "==", ALICE_UID).get());

  // Admin approves PENDING payment once
  await assertSucceeds(
    adminDb.collection("payment_requests").doc("pay_1").update({
      status: "APPROVED",
      adminNote: "Verified Easypaisa receipt",
      updatedAt: pastDate(1000),
    })
  );

  // Duplicate approval/transition on already-APPROVED payment must FAIL
  await assertFails(
    adminDb.collection("payment_requests").doc("pay_1").update({
      status: "APPROVED",
      adminNote: "Duplicate click",
      updatedAt: pastDate(500),
    })
  );
});

test("Subscription duplicate prevention, referrals, and immutable ledger", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(4000);

  // Self-referral must fail
  await assertFails(
    aliceDb.collection("referrals").doc("ref_self").set({
      referrerUserId: ALICE_UID,
      referrerCode: "SSALICE1",
      referredUserId: ALICE_UID,
      referredName: "Alice",
      activeEligible: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Deterministic subscription doc ID prevents duplicates
  await assertSucceeds(
    aliceDb.collection("subscription_payments").doc(`${ALICE_UID}_2026-11`).set({
      userId: ALICE_UID,
      userName: "Alice",
      billingMonth: "2026-11",
      cycleId: "cycle_2026-11",
      amountPaid: 100,
      paymentMethod: "WALLET",
      status: "PAID",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-deterministic doc ID is rejected
  await assertFails(
    aliceDb.collection("subscription_payments").doc("random_dup_id").set({
      userId: ALICE_UID,
      userName: "Alice",
      billingMonth: "2026-11",
      cycleId: "cycle_2026-11",
      amountPaid: 100,
      paymentMethod: "WALLET",
      status: "PAID",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ledger entries are strictly immutable (no update/delete even by admin)
  await assertSucceeds(
    adminDb.collection("ledger_entries").doc("led_1").set({
      userId: ALICE_UID,
      entryType: "CREDIT",
      category: "PROFIT_DISTRIBUTED",
      amount: 180,
      balanceAfter: 180,
      referenceId: "sale_1",
      description: "Batch 1 net profit share",
      createdAt: now,
    })
  );
  await assertFails(adminDb.collection("ledger_entries").doc("led_1").update({ amount: 9999 }));
  await assertFails(adminDb.collection("ledger_entries").doc("led_1").delete());
});
