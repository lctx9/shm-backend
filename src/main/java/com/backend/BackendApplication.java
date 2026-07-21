package com.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.TrackRepository;
import com.backend.repository.RoundRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.entity.HackathonEvent;
import com.backend.entity.Track;
import com.backend.entity.Round;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.User;
import com.backend.entity.Team;
import com.backend.entity.TeamMember;
import com.backend.entity.enums.TeamType;
import com.backend.entity.enums.MemberRole;
import java.time.LocalDateTime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner seedOverlappingEvent(
			HackathonEventRepository eventRepository,
			TrackRepository trackRepository,
			RoundRepository roundRepository,
			TrackRoundMatrixRepository matrixRepository,
			UserRepository userRepository,
			TeamRepository teamRepository,
			TeamMemberRepository teamMemberRepository) {
		return args -> {
			if (!eventRepository.existsByName("Overlap Test 2026")) {
				HackathonEvent event = HackathonEvent.builder()
						.name("Overlap Test 2026")
						.description("Sự kiện test trùng thời gian với Spring 2026")
						.season(com.backend.entity.enums.Season.SUMMER)
						.year(2026)
						.regStartDate(LocalDateTime.of(2026, 7, 1, 8, 0))
						.regEndDate(LocalDateTime.of(2026, 8, 14, 23, 59))
						.eventStartDate(LocalDateTime.of(2026, 7, 25, 8, 0))
						.eventEndDate(LocalDateTime.of(2026, 7, 28, 18, 0))
						.isActive(true)
						.structureInitialized(true)
						.build();
				event = eventRepository.save(event);

				Track track = Track.builder()
						.event(event)
						.name("Test Track")
						.description("Track cho sự kiện test")
						.build();
				track = trackRepository.save(track);

				Round r1 = Round.builder()
						.event(event)
						.name("Vòng Ý Tưởng")
						.orderIndex(1)
						.build();
				r1 = roundRepository.save(r1);

				Round r2 = Round.builder()
						.event(event)
						.name("Vòng Chung Kết")
						.orderIndex(2)
						.build();
				r2 = roundRepository.save(r2);

				TrackRoundMatrix m1 = TrackRoundMatrix.builder()
						.track(track)
						.round(r1)
						.guidelineUrl("https://example.com/guideline1.pdf")
						.submissionDeadline(LocalDateTime.of(2026, 7, 26, 23, 59))
						.scoringCriteriaJson("[]")
						.build();
				matrixRepository.save(m1);

				TrackRoundMatrix m2 = TrackRoundMatrix.builder()
						.track(track)
						.round(r2)
						.guidelineUrl("https://example.com/guideline2.pdf")
						.submissionDeadline(LocalDateTime.of(2026, 7, 28, 18, 0))
						.scoringCriteriaJson("[]")
						.build();
				matrixRepository.save(m2);
			}
			if (!eventRepository.existsByName("Winter 2026 Test")) {
				HackathonEvent event2 = HackathonEvent.builder()
						.name("Winter 2026 Test")
						.description("Sự kiện test KHÔNG trùng thời gian (để test tham gia nhiều sự kiện)")
						.season(com.backend.entity.enums.Season.FALL)
						.year(2026)
						.regStartDate(LocalDateTime.of(2026, 7, 1, 8, 0))
						.regEndDate(LocalDateTime.of(2026, 10, 15, 23, 59))
						.eventStartDate(LocalDateTime.of(2026, 11, 1, 8, 0))
						.eventEndDate(LocalDateTime.of(2026, 11, 4, 18, 0))
						.isActive(true)
						.structureInitialized(true)
						.build();
				event2 = eventRepository.save(event2);

				Track track2 = Track.builder()
						.event(event2)
						.name("Winter Track")
						.description("Track cho sự kiện Winter")
						.build();
				track2 = trackRepository.save(track2);

				Round r3 = Round.builder()
						.event(event2)
						.name("Vòng Ý Tưởng")
						.orderIndex(1)
						.build();
				r3 = roundRepository.save(r3);

				Round r4 = Round.builder()
						.event(event2)
						.name("Vòng Chung Kết")
						.orderIndex(2)
						.build();
				r4 = roundRepository.save(r4);

				TrackRoundMatrix m3 = TrackRoundMatrix.builder()
						.track(track2)
						.round(r3)
						.guidelineUrl("https://example.com/guideline3.pdf")
						.submissionDeadline(LocalDateTime.of(2026, 11, 2, 23, 59))
						.scoringCriteriaJson("[]")
						.build();
				matrixRepository.save(m3);

				TrackRoundMatrix m4 = TrackRoundMatrix.builder()
						.track(track2)
						.round(r4)
						.guidelineUrl("https://example.com/guideline4.pdf")
						.submissionDeadline(LocalDateTime.of(2026, 11, 4, 18, 0))
						.scoringCriteriaJson("[]")
						.build();
				matrixRepository.save(m4);

				// Seed a team in Winter 2026 Test for member.beta@seal.dev to check multi-team list UI
				User betaUser = userRepository.findByEmail("member.beta@seal.dev").orElse(null);
				if (betaUser != null) {
					Team winterTeam = Team.builder()
							.name("Winter Beta Team")
							.description("Đội thi của member.beta tại giải Winter 2026")
							.type(TeamType.PUBLIC)
							.event(event2)
							.track(track2)
							.build();
					winterTeam = teamRepository.save(winterTeam);

					TeamMember member = TeamMember.builder()
							.team(winterTeam)
							.user(betaUser)
							.role(MemberRole.LEADER)
							.build();
					teamMemberRepository.save(member);
				}
			}
		};
	}
}