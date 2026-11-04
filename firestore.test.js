const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_host";
const BOB_UID = "bob_player";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("rooms").get());
});

test("Authenticated host: can create valid room", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room123").set({
      roomId: "room123",
      roomCode: "HR7X29",
      hostId: ALICE_UID,
      status: "LOBBY",
      matchDurationMinutes: 10,
      currentRound: 1,
      totalRounds: 3,
      createdAt: new Date(),
    })
  );
});

test("Another user: cannot forge hostId on room creation", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("rooms").doc("room_forge").set({
      roomId: "room_forge",
      roomCode: "FORGE1",
      hostId: ALICE_UID,
      status: "LOBBY",
      matchDurationMinutes: 10,
      currentRound: 1,
      totalRounds: 3,
      createdAt: new Date(),
    })
  );
});

test("Authenticated player: can join room as player", async () => {
  // Seed room
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("room123").set({
      roomId: "room123",
      roomCode: "HR7X29",
      hostId: ALICE_UID,
      status: "LOBBY",
      matchDurationMinutes: 10,
      currentRound: 1,
      totalRounds: 3,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("rooms").doc("room123").collection("players").doc(BOB_UID).set({
      userId: BOB_UID,
      nickname: "BobArt",
      avatarColor: "#00E5FF",
      score: 0,
      isHost: false,
      isSpectator: false,
      hasGuessedThisRound: false,
      joinedAt: new Date(),
    })
  );
});

test("Drawer: can write strokes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room123").collection("strokes").doc("stroke1").set({
      strokeId: "stroke1",
      drawerId: ALICE_UID,
      color: -16711936,
      brushWidth: 8.0,
      isEraser: false,
      pointsJson: JSON.stringify([{ x: 0.1, y: 0.2 }]),
      createdAt: new Date(),
    })
  );
});

test("Guesser: cannot impersonate drawer on stroke creation", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("rooms").doc("room123").collection("strokes").doc("stroke_fake").set({
      strokeId: "stroke_fake",
      drawerId: ALICE_UID,
      color: -16711936,
      brushWidth: 8.0,
      isEraser: false,
      pointsJson: "[]",
      createdAt: new Date(),
    })
  );
});
