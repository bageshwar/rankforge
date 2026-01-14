// Test data based on actual API responses from localhost:8080
// This file contains expected values for fine-grained assertions

export const EXPECTED_RANKINGS = {
  totalPlayers: 11,
  totalGames: 2,
  totalRounds: 39,
  players: [
    {
      rank: 1661,
      playerName: "Mai Omelette Khaunga",
      playerId: "[U:1:1219143518]",
      kills: 62,
      deaths: 19,
      assists: 7,
      killDeathRatio: 3.263157894736842,
      headshotKills: 26,
      headshotPercentage: 41.935483870967744,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 9101.0,
      gamesPlayed: 2
    },
    {
      rank: 1246,
      playerName: "k1d",
      playerId: "[U:1:1090227400]",
      kills: 36,
      deaths: 33,
      assists: 7,
      killDeathRatio: 1.0909090909090908,
      headshotKills: 13,
      headshotPercentage: 36.11111111111111,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 4647.0,
      gamesPlayed: 2
    },
    {
      rank: 1241,
      playerName: "the nucLeus",
      playerId: "[U:1:1211958118]",
      kills: 16,
      deaths: 15,
      assists: 5,
      killDeathRatio: 1.0666666666666667,
      headshotKills: 7,
      headshotPercentage: 43.75,
      roundsPlayed: 30,
      clutchesWon: 0,
      damageDealt: 1903.0,
      gamesPlayed: 2
    },
    {
      rank: 1220,
      playerName: "Adkins#Keep Calm",
      playerId: "[U:1:216478675]",
      kills: 26,
      deaths: 28,
      assists: 12,
      killDeathRatio: 0.9285714285714286,
      headshotKills: 11,
      headshotPercentage: 42.30769230769231,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 4107.0,
      gamesPlayed: 2
    },
    {
      rank: 1217,
      playerName: "_m3th0d",
      playerId: "[U:1:1114723128]",
      kills: 25,
      deaths: 27,
      assists: 18,
      killDeathRatio: 0.9259259259259259,
      headshotKills: 10,
      headshotPercentage: 40.0,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 3681.0,
      gamesPlayed: 2
    },
    {
      rank: 1212,
      playerName: "[[LEGEND KILLER]] _i_",
      playerId: "[U:1:129501892]",
      kills: 27,
      deaths: 29,
      assists: 7,
      killDeathRatio: 0.9310344827586207,
      headshotKills: 9,
      headshotPercentage: 33.33333333333333,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 4156.0,
      gamesPlayed: 2
    },
    {
      rank: 1204,
      playerName: "raksh",
      playerId: "[U:1:1222942858]",
      kills: 22,
      deaths: 27,
      assists: 6,
      killDeathRatio: 0.8148148148148148,
      headshotKills: 11,
      headshotPercentage: 50.0,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 3025.0,
      gamesPlayed: 2
    },
    {
      rank: 1176,
      playerName: "Khanjer",
      playerId: "[U:1:1098204826]",
      kills: 19,
      deaths: 29,
      assists: 11,
      killDeathRatio: 0.6551724137931034,
      headshotKills: 10,
      headshotPercentage: 52.63157894736842,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 3084.0,
      gamesPlayed: 2
    },
    {
      rank: 1164,
      playerName: "HwoaranG",
      playerId: "[U:1:107493695]",
      kills: 20,
      deaths: 32,
      assists: 10,
      killDeathRatio: 0.625,
      headshotKills: 9,
      headshotPercentage: 45.0,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 2789.0,
      gamesPlayed: 2
    },
    {
      rank: 1160,
      playerName: "PARROT",
      playerId: "[U:1:1017449331]",
      kills: 19,
      deaths: 26,
      assists: 10,
      killDeathRatio: 0.7307692307692307,
      headshotKills: 4,
      headshotPercentage: 21.052631578947366,
      roundsPlayed: 39,
      clutchesWon: 0,
      damageDealt: 2443.0,
      gamesPlayed: 2
    },
    {
      rank: 1128,
      playerName: "Wasuli Bhai !!!!",
      playerId: "[U:1:1026155000]",
      kills: 6,
      deaths: 13,
      assists: 3,
      killDeathRatio: 0.46153846153846156,
      headshotKills: 3,
      headshotPercentage: 50.0,
      roundsPlayed: 15,
      clutchesWon: 0,
      damageDealt: 932.0,
      gamesPlayed: 2
    }
  ]
};

export const EXPECTED_GAMES = [
  {
    id: 2,
    map: "de_ancient",
    team1Score: 13,
    team2Score: 2,
    score: "13 - 2",
    mode: "competitive",
    duration: "22",
    formattedDuration: "22 min",
    gameDate: "2026-01-11T17:50:51.387150Z",
    totalRounds: 15,
    players: [
      "[[LEGEND KILLER]] _i_",
      "_m3th0d",
      "Adkins#Keep Calm",
      "HwoaranG",
      "k1d",
      "Khanjer",
      "Mai Omelette Khaunga",
      "PARROT",
      "raksh",
      "the nucLeus",
      "Wasuli Bhai !!!!"
    ]
  },
  {
    id: 1,
    map: "de_anubis",
    team1Score: 13,
    team2Score: 11,
    score: "13 - 11",
    mode: "competitive",
    duration: "38",
    formattedDuration: "38 min",
    gameDate: "2026-01-11T17:26:22.670587Z",
    totalRounds: 24,
    players: [
      "[[LEGEND KILLER]] _i_",
      "_m3th0d",
      "Adkins#Keep Calm",
      "HwoaranG",
      "k1d",
      "Khanjer",
      "Mai Omelette Khaunga",
      "PARROT",
      "raksh",
      "the nucLeus",
      "Wasuli Bhai !!!!"
    ]
  }
];

export const EXPECTED_GAME_1_DETAILS = {
  id: 1,
  map: "de_anubis",
  ctScore: 13,
  tScore: 11,
  totalRounds: 24,
  accolades: 10, // Based on API response
  playerStatsCount: 11, // Based on API response
  // Top player stats from API (sorted by kills descending)
  topPlayers: [
    { playerName: "Mai Omelette Khaunga", kills: 36, deaths: 14, assists: 4, damage: 3422, headshotPercentage: 44.44 },
    { playerName: "k1d", kills: 25, deaths: 18, assists: 6, damage: 2483, headshotPercentage: 40.0 },
    { playerName: "Adkins#Keep Calm", kills: 19, deaths: 15, assists: 7, damage: 2130, headshotPercentage: 42.11 },
  ],
  // Accolades from API
  accolades: [
    { type: "4k", playerName: "Mai Omelette Khaunga", value: 4.0 },
    { type: "deaths", playerName: "HwoaranG", value: 22.0 },
    { type: "chickenskilled", playerName: "raksh", value: 4.0 },
    { type: "assists", playerName: "_m3th0d", value: 14.0 },
    { type: "burndamage", playerName: "PARROT", value: 37.0 },
    { type: "killreward", playerName: "Adkins#Keep Calm", value: 5700.0 },
    { type: "cashspent", playerName: "[[LEGEND KILLER]] _i_", value: 62350.0 },
    { type: "4k", playerName: "the nucLeus", value: 1.0 },
    { type: "3k", playerName: "k1d", value: 3.0 },
    { type: "hsp", playerName: "Khanjer", value: 64.71 },
  ]
};

export const EXPECTED_GAME_2_DETAILS = {
  id: 2,
  map: "de_ancient",
  ctScore: 13,
  tScore: 2,
  totalRounds: 15,
  accolades: 11, // Based on API response (21 total - 10 from game 1)
  playerStatsCount: 11
};

export const EXPECTED_ROUND_1_GAME_1 = {
  gameId: 1,
  roundNumber: 1,
  winnerTeam: "CT",
  totalKills: 8,
  totalAssists: 6,
  headshotKills: 7,
  bombPlanted: false,
  bombDefused: false,
  bombExploded: false,
  eventsCount: 43,
  durationMs: 59331
};
