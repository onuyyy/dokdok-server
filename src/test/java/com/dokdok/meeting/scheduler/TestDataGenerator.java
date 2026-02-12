package com.dokdok.meeting.scheduler;

import com.dokdok.book.entity.Book;
import com.dokdok.book.repository.BookRepository;
import com.dokdok.gathering.entity.Gathering;
import com.dokdok.gathering.entity.GatheringStatus;
import com.dokdok.gathering.repository.GatheringRepository;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.meeting.entity.MeetingLocation;
import com.dokdok.meeting.entity.MeetingStatus;
import com.dokdok.meeting.repository.MeetingRepository;
import com.dokdok.user.entity.User;
import com.dokdok.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 성능 테스트를 위한 대량 Meeting 데이터 생성 유틸
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataGenerator {

    private final MeetingRepository meetingRepository;
    private final GatheringRepository gatheringRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    private User testUser;
    private Book testBook;
    private Gathering testGathering;

    /**
     * 지정된 개수만큼 만료된(CONFIRMED 상태) Meeting 데이터 생성
     * 
     * @param count 생성할 Meeting 개수
     * @return 생성된 Meeting 개수
     */
    @Transactional
    public int generateExpiredMeetings(int count) {
        log.info("Starting to generate {} expired meetings...", count);
        long startTime = System.currentTimeMillis();

        // 1. 테스트용 User, Book, Gathering 생성 (한 번만)
        ensureTestDataExists();

        // 2. Meeting 대량 생성
        List<Meeting> meetings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < count; i++) {
            Meeting meeting = Meeting.builder()
                    .gathering(testGathering)
                    .book(testBook)
                    .meetingLeader(testUser)
                    .meetingName("성능테스트 모임 " + i)
                    .location(MeetingLocation.builder()
                            .name("테스트 장소 " + i)
                            .address("테스트 주소 " + i)
                            .latitude(37.0)
                            .longitude(127.0)
                            .build())
                    .maxParticipants(10)
                    .meetingStatus(MeetingStatus.CONFIRMED)
                    // 종료 시간을 과거로 설정 (1시간 ~ 24시간 전)
                    .meetingStartDate(now.minusHours(25))
                    .meetingEndDate(now.minusHours(1))
                    .build();
            
            meetings.add(meeting);
            
            // 배치 사이즈마다 flush (메모리 효율)
            if (i % 100 == 0 && i > 0) {
                meetingRepository.saveAll(meetings);
                meetingRepository.flush();
                meetings.clear();
                log.info("Generated {} meetings...", i);
            }
        }
        
        // 남은 데이터 저장
        if (!meetings.isEmpty()) {
            meetingRepository.saveAll(meetings);
            meetingRepository.flush();
        }

        long endTime = System.currentTimeMillis();
        log.info("Successfully generated {} expired meetings in {}ms", 
                 count, (endTime - startTime));

        return count;
    }

    /**
     * 테스트 데이터 존재 여부 확인 및 생성
     */
    private void ensureTestDataExists() {
        if (testUser == null) {
            testUser = createTestUser();
        }
        if (testBook == null) {
            testBook = createTestBook();
        }
        if (testGathering == null) {
            testGathering = createTestGathering();
        }
    }

    /**
     * 생성된 테스트 데이터 삭제
     */
    @Transactional
    public void cleanupTestData() {
        log.info("Cleaning up test data...");
        
        // Meeting 데이터 삭제 (이름에 "성능테스트"가 포함된 것들)
        List<Meeting> allMeetings = meetingRepository.findAll();
        List<Meeting> testMeetings = allMeetings.stream()
                .filter(m -> m.getMeetingName() != null && m.getMeetingName().contains("성능테스트"))
                .toList();
        
        if (!testMeetings.isEmpty()) {
            meetingRepository.deleteAll(testMeetings);
            log.info("Cleaned up {} test meetings", testMeetings.size());
        }
        
        // 테스트 데이터 참조 초기화
        testUser = null;
        testBook = null;
        testGathering = null;
    }

    private User createTestUser() {
        // 기존에 테스트 유저가 있는지 확인
        List<User> existingUsers = userRepository.findAll();
        User existing = existingUsers.stream()
                .filter(u -> u.getUserEmail() != null && u.getUserEmail().equals("scheduler-test@dokdok.com"))
                .findFirst()
                .orElse(null);
        
        if (existing != null) {
            log.info("Using existing test user: {}", existing.getId());
            return existing;
        }
        
        // 없으면 새로 생성
        User user = User.builder()
                .userEmail("scheduler-test@dokdok.com")
                .nickname("스케줄러테스트")
                .kakaoId(999999999L)
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
        
        User saved = userRepository.save(user);
        log.info("Created test user: {}", saved.getId());
        return saved;
    }

    private Book createTestBook() {
        // 기존에 테스트 책이 있는지 확인
        List<Book> existingBooks = bookRepository.findAll();
        Book existing = existingBooks.stream()
                .filter(b -> b.getIsbn() != null && b.getIsbn().equals("TEST-SCHEDULER-ISBN"))
                .findFirst()
                .orElse(null);
        
        if (existing != null) {
            log.info("Using existing test book: {}", existing.getId());
            return existing;
        }
        
        // 없으면 새로 생성
        Book book = Book.builder()
                .bookName("스케줄러 성능 테스트용 책")
                .author("테스트 저자")
                .publisher("테스트 출판사")
                .isbn("TEST-SCHEDULER-ISBN")
                .thumbnail("https://example.com/book.jpg")
                .build();
        
        Book saved = bookRepository.save(book);
        log.info("Created test book: {}", saved.getId());
        return saved;
    }

    private Gathering createTestGathering() {
        // 기존에 테스트 모임이 있는지 확인
        List<Gathering> existingGatherings = gatheringRepository.findAll();
        Gathering existing = existingGatherings.stream()
                .filter(g -> g.getGatheringName() != null && g.getGatheringName().equals("스케줄러 테스트 모임"))
                .findFirst()
                .orElse(null);
        
        if (existing != null) {
            log.info("Using existing test gathering: {}", existing.getId());
            return existing;
        }
        
        // 없으면 새로 생성
        String uniqueCode = UUID.randomUUID().toString().substring(0, 8);
        Gathering gathering = Gathering.builder()
                .gatheringName("스케줄러 테스트 모임")
                .description("성능 테스트를 위한 모임입니다")
                .invitationLink("SCHEDULER-TEST-" + uniqueCode)
                .gatheringLeader(testUser)
                .gatheringStatus(GatheringStatus.ACTIVE)
                .build();
        
        Gathering saved = gatheringRepository.save(gathering);
        log.info("Created test gathering: {}", saved.getId());
        return saved;
    }
}
