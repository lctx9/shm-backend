# 🚀 SEAL HACKATHON - COMPLETE TEST DATA PACKAGE

## 📦 What You Got

```
seal/
├── test_data.sql                          ✅ SQL INSERT statements (50 records)
├── TEST_DATA_README.md                    ✅ Complete import guide
├── TEST_DATA_STATS.md                     ✅ Data statistics & coverage
└── SEAL_API_Tests.postman_collection.json ✅ Postman API collection
```

---

## 🎯 Quick Start (5 minutes)

### Step 1: Import Database
```bash
# Option 1: PostgreSQL CLI
psql -U postgres -d seal_db -f test_data.sql

# Option 2: pgAdmin GUI
# - Tools → Query Tool → Paste content → Execute

# Option 3: Spring Boot Auto-import
# - Copy test_data.sql to src/main/resources/data.sql
# - Add to application.properties:
#   spring.jpa.hibernate.ddl-auto=create-drop
#   spring.sql.init.mode=always
```

### Step 2: Start Backend
```bash
cd shm-backend
mvn clean spring-boot:run
```

### Step 3: Start Frontend
```bash
cd shm-frontend
npm install
npm start
```

### Step 4: Test with Postman
```bash
# Import SEAL_API_Tests.postman_collection.json into Postman
# Replace YOUR_TOKEN_HERE with actual token from login response
```

---

## 📊 Test Data Overview

| Entity | Count | Status |
|--------|-------|--------|
| Users | 13 | ✅ Diverse roles |
| Roles | 5 | ✅ All types |
| Events | 2 | ✅ Different statuses |
| Tracks | 4 | ✅ Multiple types |
| Rounds | 3 | ✅ Full schedule |
| Teams | 4 | ✅ Mixed status |
| Submissions | 4 | ✅ Various stages |
| Judge Scores | 5 | ✅ Real values |
| Calibrations | 2 | ✅ Reference data |
| Prizes | 2 | ✅ Winners |
| Audit Logs | 2 | ✅ Action trail |
| **TOTAL** | **~50** | **✅ Production-ready** |

---

## 🔐 Test Login Accounts

### Admin/Coordinator
```
Email: coordinator@seal.com
Pass:  coor123
Role:  COORDINATOR
```

### Judge
```
Email: judge1@seal.com
Pass:  judge123
Role:  JUDGE (Internal)
```

### Student (Active)
```
Email: student1@fptu.edu.vn
Pass:  student123
Role:  STUDENT (Status: ACTIVE)
```

### Student (Pending Approval)
```
Email: pending1@fptu.edu.vn
Pass:  student123
Role:  STUDENT (Status: PENDING - needs approval)
```

👉 **See TEST_DATA_README.md for all 13 user accounts**

---

## 📝 Test Workflows

### Workflow 1: Event Creation
```
1. Login as Coordinator
   POST /api/public/auth/login
   
2. Create Event
   POST /api/events
   
3. Add Tracks
   POST /api/events/{id}/tracks
   
4. Add Rounds
   POST /api/events/{id}/rounds
   
5. Generate Matrix
   POST /api/events/{id}/init-matrix
```

### Workflow 2: Team Formation
```
1. Login as Student
   POST /api/public/auth/login
   
2. Search other students
   GET /api/public/users/search?keyword=...
   
3. Create Team
   POST /api/teams
   
4. Add Members
   POST /api/teams/members
   
5. Verify created team
   GET /api/teams
```

### Workflow 3: Submission & Scoring
```
1. Create Submission
   POST /api/submissions
   
2. Submit (change status)
   PUT /api/submissions/{id}/submit
   
3. Login as Judge
   GET /api/submissions (for review)
   
4. Score Submission
   POST /api/judge-scores
   
5. View Results
   GET /api/teams/rankings
```

### Workflow 4: User Approval
```
1. Login as Coordinator
2. View pending users
   GET /api/coordinator/users/pending
   
3. Approve user
   PUT /api/coordinator/users/approve
   { "userId": "...", "approved": true }
   
4. Reject user (optional)
   PUT /api/coordinator/users/approve
   { "userId": "...", "approved": false, "reason": "..." }
```

---

## 🧪 Key Test Scenarios

### ✅ Scenario A: Happy Path
- Coordinator creates event ✓
- Students create teams ✓
- Teams submit solutions ✓
- Judges score submissions ✓
- System calculates rankings ✓

### ✅ Scenario B: User Approval Flow
- New student registers
- Status = PENDING ✓
- Coordinator sees pending users ✓
- Coordinator approves ✓
- Status = ACTIVE ✓

### ✅ Scenario C: Scoring Calibration
- Coordinator uploads calibration samples ✓
- Judges score calibration pieces ✓
- Reference scores stored ✓
- Used for judge calibration ✓

### ✅ Scenario D: Multi-Round Competition
- Round 1: All teams submit
- Top teams advance ✓
- Round 2: Semifinals
- Final round ranking ✓

---

## 🔍 Verification Queries

After import, verify with:

```sql
-- Count all records
SELECT 'Users' as entity, COUNT(*) as count FROM users
UNION ALL
SELECT 'Teams', COUNT(*) FROM teams
UNION ALL
SELECT 'Submissions', COUNT(*) FROM submissions
UNION ALL
SELECT 'JudgeScores', COUNT(*) FROM judge_scores;

-- Check event structure
SELECT e.name, COUNT(t.id) as tracks, COUNT(DISTINCT r.id) as rounds
FROM hackathon_events e
LEFT JOIN tracks t ON e.id = t.event_id
LEFT JOIN rounds r ON e.id = r.event_id
GROUP BY e.id;

-- Verify team members
SELECT t.name, COUNT(tm.id) as member_count
FROM teams t
LEFT JOIN team_members tm ON t.id = tm.team_id
GROUP BY t.id;

-- Check scoring
SELECT s.id, t.name, COUNT(js.id) as score_count
FROM submissions s
LEFT JOIN teams t ON s.team_id = t.id
LEFT JOIN judge_scores js ON s.id = js.submission_id
GROUP BY s.id, t.name;
```

---

## 📱 Frontend Testing Checklist

- [ ] Login with all 5 user types works
- [ ] Home page shows event info correctly
- [ ] Coordinator can view pending users
- [ ] Coordinator can approve/reject users
- [ ] Student can create team
- [ ] Student can add team members
- [ ] Student can submit solution
- [ ] Judge can view submissions
- [ ] Judge can enter scores
- [ ] Rankings calculate correctly
- [ ] Audit logs record actions

---

## 🔧 Troubleshooting

### Problem: "Foreign key constraint failed"
**Solution:** Ensure all UUIDs exist before importing. Check test_data.sql order.

### Problem: "Duplicate key value" error
**Solution:** Clear database first: `DROP SCHEMA public CASCADE; CREATE SCHEMA public;`

### Problem: Password login fails
**Solution:** Passwords are plain text. If using BCrypt, run `/api/public/migrate-passwords`

### Problem: Can't find user in search
**Solution:** User must have ACTIVE status. Check user status in database.

### Problem: Token expired
**Solution:** Get new token by logging in again. Mock JWT lasts 1 hour.

---

## 📚 Documentation Files

1. **test_data.sql**
   - Raw SQL INSERT statements
   - 50 records across all entities
   - Ready to execute

2. **TEST_DATA_README.md**
   - Complete import guide
   - All test credentials
   - UUID references

3. **TEST_DATA_STATS.md**
   - Data statistics
   - Relationship maps
   - Timeline & schedule

4. **SEAL_API_Tests.postman_collection.json**
   - Ready-to-use API tests
   - 20+ endpoint examples
   - All workflows covered

---

## ✨ Features Demonstrated

✅ **Authentication & Authorization**
- 5 different roles
- Multiple user statuses
- Token-based auth

✅ **Event Management**
- Multi-season support
- Event lifecycle
- Track & round configuration

✅ **Team Management**
- Team creation
- Member assignment
- Status tracking

✅ **Submission System**
- Draft submissions
- Multiple rounds
- Status transitions

✅ **Scoring System**
- Multi-criteria evaluation
- Calibration reference
- Judge assignments

✅ **Audit Trail**
- Action logging
- Status changes
- User tracking

---

## 🎓 Learning Resources

**Backend (Java/Spring):**
- Event entities & relationships
- Repository patterns
- Service layer logic
- Controller implementations

**Frontend (React):**
- Form handling
- API integration
- State management
- Conditional rendering

**Database:**
- PostgreSQL schemas
- Foreign key relationships
- UUID usage
- Timestamp management

---

## 📞 Support

If you encounter issues:

1. Check **TEST_DATA_README.md** for import issues
2. Check **TEST_DATA_STATS.md** for data structure
3. Verify database connection
4. Check Spring Boot logs for errors
5. Validate Postman environment setup

---

## 🎉 You're All Set!

You now have:
✅ Complete test database setup
✅ User accounts for all roles
✅ Real business scenarios
✅ API test collection
✅ Comprehensive documentation

**Next Steps:**
1. Import SQL into database
2. Start backend
3. Start frontend
4. Login with test accounts
5. Execute test workflows
6. Use Postman for API testing

---

**Created:** July 2, 2026  
**SEAL System v1.0** 🚀  
**Status:** Production-Ready ✅

