# QA ধাপ — হাতে ক্লিক করে পরীক্ষা (UI)

ICT Lab Resource Booking System · Phase 9 এর ডেমো হাঁটার স্ক্রিপ্ট

> **বাটন, ট্যাব আর স্ক্রিনের মেসেজগুলো ইচ্ছে করেই ইংরেজিতে রাখা** — পর্দায় ঠিক
> এই লেখাটাই দেখা যায়, অনুবাদ করলে মিলিয়ে দেখা আর সম্ভব হতো না।
> **Expect:** মানে ওই ধাপে যা হওয়া উচিত।

---

## ০. প্রতিবার শুরুর আগে ডাটা রিসেট

```
mysql -u root < db\schema.sql
mysql -u root < db\seed.sql
mvn javafx:run
```

Laragon এর `mysql.exe` PATH এ নেই, তাই পুরো পাথ দিয়ে ডাকতে হবে —
`C:\laragon\bin\mysql\...\bin\mysql.exe`।

`schema.sql` আগে টেবিল মুছে নতুন বানায়, তারপর `seed.sql` ডেমো ডাটা ঢালে।

**রিসেটের পরের স্টক।** seed এ এখন **৪টা subject**, **৭৬টা material**, **৯০৮টা unit** —
সবগুলোই `AVAILABLE`, **একটাও `DAMAGED` নয়**। তাই নিচের ধাপগুলো ইচ্ছে করেই
`EEE Lab` এর কম সংখ্যার material গুলো দিয়ে করা, যাতে গোনা যায়:

| Subject | Material | খালি আছে |
|---|---|---|
| EEE Lab | Breadboard (830 points) | **4** |
| EEE Lab | Arduino Uno (R3) | **4** |
| EEE Lab | Sensor (DHT11) | **3** |
| EEE Lab | Digital Multimeter | **5** |
| EEE Lab | Soldering Iron (60W) | **5** |

> **`0 available` পরীক্ষা করার নিয়ম বদলেছে:** seed এ আর কোনো DAMAGED ইউনিট নেই,
> তাই শূন্য স্টক **হাতে বানাতে হবে** — কোনো material এর সব ইউনিট একটা রিকোয়েস্টে
> বুক করে ফেললেই সেটা `0 available` দেখাবে (ধাপ ১৪ এভাবেই করা)।

চারটা অ্যাকাউন্টের পাসওয়ার্ড একই: `password123` — শুধু `admin@mbstu.ac.bd` এর
পাসওয়ার্ড `admin123`। আর আজকের তারিখ ধরা হয়েছে **2026-10-24**।

| অ্যাকাউন্ট | ইমেইল | role |
|---|---|---|
| Lab Administrator | `admin@lab.edu` | ADMIN |
| Lab Assistant | `admin@mbstu.ac.bd` | ADMIN |
| Al-Shahariar  | `shahariar@lab.edu` | STUDENT |
| Nusrat Jahan | `nusrat@lab.edu` | STUDENT |

---

## ক. লগইন আর রেজিস্ট্রেশন

- [ ] **১.** অ্যাপ চালু হলে লগইন স্ক্রিন আসবে। `shahariar@lab.edu` + ভুল পাসওয়ার্ড `wrongpass` দিয়ে লগইন।
      **Expect:** লাল লেখায় `Invalid email or password`। ইমেইলটা আছে কি নেই সেটা
      আলাদা করে বলা হয় না — ইচ্ছে করেই, নাহলে কোন ইমেইল রেজিস্টার্ড তা ফাঁস হয়।

- [ ] **২.** Register স্ক্রিনে গিয়ে নাম `Test Student`, ইমেইল `shahariar@lab.edu`, পাসওয়ার্ড `password123`।
      **Expect:** `This email is already registered`.

- [ ] **৩.** একই ফর্মে ইমেইল বদলে `test@lab.edu`, পাসওয়ার্ড `short` (৮ অক্ষরের কম)।
      **Expect:** `Password must be at least 8 characters`.

- [ ] **৪.** নাম খালি রেখে চেষ্টা → **Expect:** `Full name is required`।
      ইমেইলে `abc` লিখে চেষ্টা → **Expect:** `Enter a valid email address`।

- [ ] **৫.** এবার ঠিকভাবে: `Test Student` / `test@lab.edu` / `password123`।
      **Expect:** রেজিস্টার হয়ে **সোজা** ছাত্রের স্ক্রিনে ঢুকে যাবে (আলাদা করে লগইন করতে হবে না),
      উপরে হেডারে `Test Student  (STUDENT)`।

- [ ] **৬.** `Logout` → আবার `shahariar@lab.edu` / `password123` দিয়ে লগইন।
      **Expect:** ছাত্রের স্ক্রিন, হেডারে `Al-Shahariar   (STUDENT)`।

---

## খ. ছাত্রের স্বাভাবিক কাজ (happy path)

- [ ] **৭.** স্ক্রিনে ঢোকার সময় কোনো subject বাছা থাকে না, পুরো ল্যাবের স্টক দেখায়।
      **Expect:** ৭৬টা row, প্রতিটার শেষ কলামে `N available`।

- [ ] **৮.** Subject ড্রপডাউনে `EEE Lab` বাছা।
      **Expect:** শুধু ৫টা row (Breadboard, Arduino Uno, Sensor DHT11,
      Digital Multimeter, Soldering Iron)।

- [ ] **৯.** টেবিলে `Arduino Uno` বাছা, Quantity **2**, তারপর `Add to request`।
      **Expect:** সবুজ লেখায় `Arduino Uno (R3) x2 added to the request list`,
      আর Request list এ একটা row।

- [ ] **১০.** একই `Arduino Uno` আবার বাছা, Quantity **1**, আবার `Add to request`।
      **Expect:** নতুন row **হবে না** — আগের row এর Quantity **3** হয়ে যাবে।
      (এটাই cart merge নিয়ম, ডুপ্লিকেট লাইন নয়।)

- [ ] **১১.** `Sensor` (DHT11) বাছা, Quantity 1, Add। Purpose এ `Final year project`,
      Return date এ **2026-10-31**, তারপর `Submit request`।
      **Expect:** সবুজ লেখায় `Request REQ-… submitted. Check My bookings for its status.`
      আর Request list খালি হয়ে যাবে।

- [ ] **১২.** `My bookings` ট্যাব।
      **Expect:** রিকোয়েস্টটা দেখা যাবে, status `PENDING`। ওটার উপর ক্লিক করলে
      নিচে তার ২টা আইটেম লাইন আসবে।

- [ ] **১৩.** `Browse & request` ট্যাবে ফিরে `Refresh stock`।
      **Expect:** `Arduino Uno` এখন **1 available** (৪ − ৩), `Sensor (DHT11)`
      **2 available** (৩ − ১) — কারণ সাবমিটের সময় ইউনিটগুলো BOOKED হয়ে গেছে।

---

## গ. ছাত্রের ভুল ধরার পরীক্ষা (এখানেই আসল নম্বর)

- [ ] **১৪.** **আগে শূন্য স্টক বানাতে হবে** (seed এ DAMAGED ইউনিট নেই)।
      `EEE Lab` এ `Arduino Uno` এর বাকি **1** টা Quantity **1** দিয়ে Add,
      Purpose আর ভবিষ্যতের তারিখ দিয়ে Submit → সফল। তারপর `Refresh stock`।
      **Expect:** `Arduino Uno` এখন **0 available**।
      এবার ওই `Arduino Uno` বাছা, `Add to request`।
      **Expect:** লাল লেখায় `Arduino Uno (R3) has 0 available and cannot be added`,
      আর Request list এ কিছুই ঢুকবে না।

- [ ] **১৫.** `EEE Lab` এ `Sensor` (DHT11) — এখন **2** টা খালি — বাছা,
      Quantity **3**, Add। Purpose আর ভবিষ্যতের তারিখ দিয়ে `Submit request`।
      **Expect:** লাল লেখায়
      `Only 2 of 3 Sensor (DHT11) are available now. Nothing was saved.`
      **একটা জিনিসও সেভ হবে না** (পুরো লেনদেন রোলব্যাক), আর Request list অছোঁয়া
      থাকবে — যাতে সংখ্যা কমিয়ে আবার চেষ্টা করা যায়।

- [ ] **১৬.** এবার row মুছে `Sensor` (DHT11) x**2** যোগ করে Submit।
      **Expect:** সফল। (অর্থাৎ ১৫ নম্বরে সত্যিই কিছু সেভ হয়নি।)

- [ ] **১৭.** কার্টে কিছু রেখে Purpose **খালি** রেখে Submit।
      **Expect:** `Purpose is required`।

- [ ] **১৮.** Purpose দিয়ে Return date এ **আজকের** তারিখ (2026-10-24) দিয়ে Submit।
      **Expect:** `Return date must be in the future` — আজকের দিনও চলবে না।

- [ ] **১৯.** কার্ট খালি রেখে Submit।
      **Expect:** `Add at least one item to the request`।

- [ ] **২০.** কার্টে জিনিস রেখে Subject ড্রপডাউন **বদলানো**।
      **Expect:** কার্ট **খালি হয়ে যাবে** (নতুন subject = নতুন কাজ)।
      আবার কিছু যোগ করে `Show all lab stock` চাপা।
      **Expect:** কার্ট **ঠিক থাকবে** — এই দুইটার ব্যবহার আলাদা।

- [ ] **২১.** কার্টের একটা row বাছা → `Remove selected item`।
      **Expect:** ওই row টা চলে যাবে। কিছু না বাছা থাকলে কিছুই হবে না।

---

## ঘ. অ্যাডমিনের কাজ

- [ ] **২২.** `Logout` → `admin@lab.edu` / `password123`।
      **Expect:** অ্যাডমিনের স্ক্রিন, `Requests` ট্যাব,
      `Show requests with status` ড্রপডাউনে `PENDING`।

- [ ] **২৩.** Al-Shahariar এর রিকোয়েস্ট বাছা।
      **Expect:** নিচে `Items in the selected request` এ আইটেম লাইন, আর
      `Units held by this request` এ আটকে রাখা ইউনিটগুলো (serial সহ)।

- [ ] **২৪.** শুধু **একটা** আইটেম বাছা → `Approve item`।
      **Expect:** রিকোয়েস্ট এখনো `PENDING` — কারণ আরেকটা লাইনের সিদ্ধান্ত বাকি।
      (হেডারের status লাইনগুলো থেকে হিসাব হয়, হাতে বসানো হয় না।)

- [ ] **২৫.** বাকি আইটেমটা বাছা → `Reject item`।
      **Expect:** রিকোয়েস্ট `APPROVED` হবে (একটা অনুমোদিত আছে), আর **reject করা
      আইটেমের ইউনিটগুলো আবার AVAILABLE** হয়ে যাবে — নাহলে ওগুলো চিরকাল আটকে থাকত।

- [ ] **২৬.** status ড্রপডাউন `APPROVED` করে রিকোয়েস্টটা বাছা → `Record issue`।
      **Expect:** রিকোয়েস্ট আর তার ইউনিট দুইটাই `ISSUED`,
      আর রিকোয়েস্টটা `ISSUED` তালিকায় চলে যাবে।

- [ ] **২৭.** status `ISSUED` → রিকোয়েস্ট বাছা → ইউনিটের টেবিলে **Ctrl+click করে
      একটা ইউনিট** বাছা (নষ্ট হিসেবে) → `Record return`।
      **Expect:** রিকোয়েস্ট `RETURNED`; বাছা ইউনিটটা `DAMAGED`;
      **যেগুলো বাছা হয়নি সেগুলো সব `AVAILABLE`**।

- [ ] **২৮.** অন্য একটা PENDING রিকোয়েস্টে `Reject whole request`।
      **Expect:** সব PENDING লাইন REJECTED, রিকোয়েস্ট `REJECTED`,
      আর ওই রিকোয়েস্টের আটকে রাখা সব ইউনিট আবার AVAILABLE।

- [ ] **২৯.** `Inventory` ট্যাব → `Breadboard (830 points)` বাছা,
      Serial number `EEE-BB-001` দিয়ে `Add unit`।
      **Expect:** `Serial number EEE-BB-001 already exists` — ডুপ্লিকেট serial ঢুকবে না।

- [ ] **৩০.** এবার serial `EEE-BB-005` দিয়ে `Add unit`।
      **Expect:** যোগ হবে, আর Breadboard এর খালি সংখ্যা ১ বাড়বে।

- [ ] **৩১.** ধাপ ২৭ এ যে ইউনিটটা `DAMAGED` করা হয়েছিল সেটা তালিকায় বাছা →
      `Mark selected unit repaired`।
      **Expect:** status `AVAILABLE`, খালি সংখ্যা আরও ১ বাড়বে।
      (seed এ DAMAGED ইউনিট নেই, তাই ধাপ ২৭ এর বানানো ইউনিটটাই এখানে কাজে লাগে।)

---

## ঙ. দুই দিক মিলিয়ে দেখা

- [ ] **৩২.** `Logout` → আবার `shahariar@lab.edu` দিয়ে লগইন → `My bookings`।
      **Expect:** অ্যাডমিন যা যা করেছে সেই status ই দেখা যাবে
      (`APPROVED` / `ISSUED` / `RETURNED` / `REJECTED`)।
      ট্যাব ছেড়ে আবার ফিরলে নতুন করে পড়া হয়, পুরোনো তালিকা থাকে না।

- [ ] **৩৩.** পুরো অ্যাপে **জরিমানার (penalty) কোনো স্ক্রিন বা বাটন নেই** — এটাই ঠিক।
      Part I এ penalties কাটা হয়েছে, তাই প্রোপোজালে ওটা **Planned**, Implemented নয়।

---

## জানা সীমা

- ধাপ গ আর ঘ ডাটা বদলে দেয়। তাই **স্ক্রিনশট নেওয়ার আগে বা আসল ডেমোর আগে
  ধাপ ০ আবার চালাতে হবে** — নাহলে উপরের "N available" সংখ্যাগুলো আর মিলবে না।
- এটা হাতে ক্লিক করার স্ক্রিপ্ট, স্বয়ংক্রিয় টেস্ট নয়। স্বয়ংক্রিয় টেস্ট আছে
  শুধু পাসওয়ার্ড হ্যাশের ৩টা (`mvn -q clean test`), আর Phase 6b তে cart merge,
  rollback আর তারিখ যাচাইয়ের টেস্ট যোগ হবে।
- Phase 7 এর প্রোপোজালের "Testing and QA Strategy" সেকশনে এই ধাপগুলো আবার
  লেখার দরকার নেই — ওখান থেকে এই ফাইলটা রেফার করলেই হবে।
