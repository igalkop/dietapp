package com.ikop.diet.controllers;

import com.ikop.diet.model.DiaryEntry;
import com.ikop.diet.model.EntriesForDate;
import com.ikop.diet.repository.DiaryEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.CollectionAssert.assertThatCollection;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiaryControllerIT {


    @LocalServerPort
    private int port;

    private final String HOST = "localhost";

    @Autowired
    private DiaryEntryRepository diaryEntryRepository;


    @Autowired
    private TestRestTemplate testRestTemplate;


    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8");


    @BeforeEach
    public void setUp() {
        diaryEntryRepository.deleteAll();
    }


    @Test
    void testGetAllForDate() {
        LocalDate now = LocalDate.now();
        DiaryEntry diaryEntry = new DiaryEntry(null, "food1", 3.3, 2, now);
        DiaryEntry diaryEntry2 = new DiaryEntry(null, "food2", 4.5, 1, now);
        DiaryEntry diaryEntry3 = new DiaryEntry(null, "food3", 1, 1, now);
        diaryEntryRepository.saveAll(List.of(diaryEntry, diaryEntry2, diaryEntry3));


        String getForDatePath = "/diary/api/list/YEAR/MONTH/DAY";
        getForDatePath = getForDatePath.replace("YEAR", String.valueOf(now.getYear())).replace("MONTH", String.valueOf(now.getMonthValue()))
                .replace("DAY", String.valueOf(now.getDayOfMonth()));
        URI urlGetEntriesForDate = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(HOST)
                .port(port)
                .path(getForDatePath)
                .build().toUri();

        ResponseEntity<EntriesForDate> allDiaryEntriesForDate = testRestTemplate.getForEntity(urlGetEntriesForDate, EntriesForDate.class);
        assertThat(allDiaryEntriesForDate.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(allDiaryEntriesForDate.getBody().getEntries().size()).isEqualTo(3);
        assertThatCollection(allDiaryEntriesForDate.getBody().getEntries()).containsExactlyInAnyOrderElementsOf(List.of(diaryEntry, diaryEntry2, diaryEntry3));
        assertThat(allDiaryEntriesForDate.getBody().getTotalPoints()).isEqualTo(12.1);
    }

    @Test
    void testPersistEntities() {
        LocalDate date = LocalDate.now().minus(3, ChronoUnit.DAYS);
        DiaryEntry diaryEntry = new DiaryEntry(null, "food 1", 1.5, 1, date);
        DiaryEntry diaryEntry2 = new DiaryEntry(null, "food 2", 2.5, 2, date);
        DiaryEntry diaryEntry3 = new DiaryEntry(null, "food 3", 3.5, 3, date);
        List<DiaryEntry> allEntries = List.of(diaryEntry, diaryEntry2, diaryEntry3);

        URI urlGetEntriesForDate = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(HOST)
                .port(port)
                .path("/diary/api")
                .build().toUri();


        for (DiaryEntry entry : allEntries) {
            ResponseEntity<DiaryEntry> response = testRestTemplate.postForEntity(urlGetEntriesForDate, entry, DiaryEntry.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
            assertThat(response.getBody())
                    .hasNoNullFieldsOrProperties()
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(entry);

        }

        long count = diaryEntryRepository.count();
        assertThat(count).isEqualTo(3);
        assertThatCollection(diaryEntryRepository.findAll())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .isEqualTo(allEntries);
    }
}