/*
 *  Copyright (c) 2011 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#include "modules/audio_coding/codecs/isac/main/source/pitch_gain_tables.h"
#include "modules/audio_coding/codecs/isac/main/source/settings.h"

/* header file for coding tables for the pitch filter side-info in the entropy coder */
/********************* Pitch Filter Gain Coefficient Tables ************************/
/* cdf for quantized pitch filter gains */
const uint16_t WebRtcIsac_kQPitchGainCdf[255] = {
  0,  2,  4,  6,  64,  901,  903,  905,  16954,  16956,
  16961,  17360,  17362,  17364,  17366,  17368,  17370,  17372,  17374,  17411,
  17514,  17516,  17583,  18790,  18796,  18802,  20760,  20777,  20782,  21722,
  21724,  21728,  21738,  21740,  21742,  21744,  21746,  21748,  22224,  22227,
  22230,  23214,  23229,  23239,  25086,  25108,  25120,  26088,  26094,  26098,
  26175,  26177,  26179,  26181,  26183,  26185,  26484,  26507,  26522,  27705,
  27731,  27750,  29767,  29799,  29817,  30866,  30883,  30885,  31025,  31029,
  31031,  31033,  31035,  31037,  31114,  31126,  31134,  32687,  32722,  32767,
  35718,  35742,  35757,  36943,  36952,  36954,  37115,  37128,  37130,  37132,
  37134,  37136,  37143,  37145,  37152,  38843,  38863,  38897,  47458,  47467,
  47474,  49040,  49061,  49063,  49145,  49157,  49159,  49161,  49163,  49165,
  49167,  49169,  49171,  49757,  49770,  49782,  61333,  61344,  61346,  62860,
  62883,  62885,  62887,  62889,  62891,  62893,  62895,  62897,  62899,  62901,
  62903,  62905,  62907,  62909,  65496,  65498,  65500,  65521,  65523,  65525,
  65527,  65529,  65531,  65533,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,  65535,
  65535,  65535,  65535,  65535,  65535};

/* index limits and ranges */
const int16_t WebRtcIsac_kIndexLowerLimitGain[3] = {
  -7, -2, -1};

const int16_t WebRtcIsac_kIndexUpperLimitGain[3] = {
  0,  3,  1};

const uint16_t WebRtcIsac_kIndexMultsGain[2] = {
  18,  3};

/* size of cdf table */
const uint16_t WebRtcIsac_kQCdfTableSizeGain[1] = {
  256};

///////////////////////////FIXED POINT
/* mean values of pitch filter gains in FIXED point */
const int16_t WebRtcIsac_kQMeanGain1Q12[144] = {
   843,    1092,    1336,    1222,    1405,    1656,    1500,    1815,    1843,    1838,    1839,    1843,    1843,    1843,    1843,    1843,
  1843,    1843,     814,     846,    1092,    1013,    1174,    1383,    1391,    1511,    1584,    1734,    1753,    1843,    1843,    1843,
  1843,    1843,    1843,    1843,     524,     689,     777,     845,     947,    1069,    1090,    1263,    1380,    1447,    1559,    1676,
  1645,    1749,    1843,    1843,    1843,    1843,      81,     477,     563,     611,     706,     806,     849,    1012,    1192,    1128,
  1330,    1489,    1425,    1576,    1826,    1741,    1843,    1843,       0,     290,     305,     356,     488,     575,     602,     741,
   890,     835,    1079,    1196,    1182,    1376,    1519,    1506,    1680,    1843,       0,      47,      97,      69,     289,     381,
   385,     474,     617,     664,     803,    1079,     935,    1160,    1269,    1265,    1506,    1741,       0,       0,       0,       0,
   112,     120,     190,     283,     442,     343,     526,     809,     684,     935,    1134,    1020,    1265,    1506,       0,       0,
     0,       0,       0,       0,       0,     111,     256,      87,     373,     597,     430,     684,     935,     770,    1020,    1265};

const int16_t WebRtcIsac_kQMeanGain2Q12[144] = {
  1760,    1525,    1285,    1747,    1671,    1393,    1843,    1826,    1555,    1843,    1784,    1606,    1843,    1843,    1711,    1843,
  1843,    1814,    1389,    1275,    1040,    1564,    1414,    1252,    1610,    1495,    1343,    1753,    1592,    1405,    1804,    1720,
  1475,    1843,    1814,    1581,    1208,    1061,    856,    1349,    1148,    994,    1390,    1253,    1111,    1495,    1343,    1178,
  1770,    1465,    1234,    1814,    1581,    1342,    1040,    793,    713,    1053,    895,    737,    1128,    1003,    861,    1277,
  1094,    981,    1475,    1192,    1019,    1581,    1342,    1098,    855,    570,    483,    833,    648,    540,    948,    744,
  572,    1009,    844,    636,    1234,    934,    685,    1342,    1217,    984,    537,    318,    124,    603,    423,    350,
  687,    479,    322,    791,    581,    430,    987,    671,    488,    1098,    849,    597,    283,    27,        0,    397,
  222,    38,        513,    271,    124,    624,    325,    157,    737,    484,    233,    849,    597,    343,    27,        0,
  0,    141,    0,    0,    256,    69,        0,    370,    87,        0,    484,    229,    0,    597,    343,    87};

const int16_t WebRtcIsac_kQMeanGain3Q12[144] = {
  1843,    1843,    1711,    1843,    1818,    1606,    1843,    1827,    1511,    1814,    1639,    1393,    1760,    1525,    1285,    1656,
  1419,    1176,    1835,    1718,    1475,    1841,    1650,    1387,    1648,    1498,    1287,    1600,    1411,    1176,    1522,    1299,
  1040,    1419,    1176,    928,    1773,    1461,    1128,    1532,    1355,    1202,    1429,    1260,    1115,    1398,    1151,    1025,
  1172,    1080,    790,    1176,    928,    677,    1475,    1147,    1019,    1276,    1096,    922,    1214,    1010,    901,    1057,
  893,    800,    1040,    796,    734,    928,    677,    424,    1137,    897,    753,    1120,    830,    710,    875,    751,
  601,    795,    642,    583,    790,    544,    475,    677,    474,    140,    987,    750,    482,    697,    573,    450,
  691,    487,    303,    661,    394,    332,    537,    303,    220,    424,    168,    0,    737,    484,    229,    624,
  348,    153,    441,    261,    136,    397,    166,    51,        283,    27,        0,    168,    0,    0,    484,    229,
  0,    370,    57,        0,    256,    43,        0,    141,    0,        0,    27,        0,    0,    0,    0,    0};


const int16_t WebRtcIsac_kQMeanGain4Q12[144] = {
  1843,    1843,    1843,    1843,    1841,    1843,    1500,    1821,    1843,    1222,    1434,    1656,    843,    1092,    1336,    504,
  757,    1007,    1843,    1843,    1843,    1838,    1791,    1843,    1265,    1505,    1599,    965,    1219,    1425,    730,    821,
  1092,    249,    504,    757,    1783,    1819,    1843,    1351,    1567,    1727,    1096,    1268,    1409,    805,    961,    1131,
  444,    670,    843,    0,        249,    504,    1425,    1655,    1743,    1096,    1324,    1448,    822,    1019,    1199,    490,
  704,    867,    81,        450,    555,    0,    0,        249,    1247,    1428,    1530,    881,    1073,    1283,    610,    759,
  939,    278,    464,    645,    0,    200,    270,    0,    0,    0,        935,    1163,    1410,    528,    790,    1068,
  377,    499,    717,    173,    240,    274,    0,    43,        62,        0,    0,    0,    684,    935,    1182,    343,
  551,    735,    161,    262,    423,    0,    55,        27,        0,    0,    0,    0,    0,    0,    430,    684,
  935,    87,        377,    597,    0,    46,        256,    0,    0,    0,    0,    0,    0,    0,    0,    0};
