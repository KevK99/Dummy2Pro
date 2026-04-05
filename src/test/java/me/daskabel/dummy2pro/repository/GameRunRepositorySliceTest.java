package me.daskabel.dummy2pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import me.daskabel.dummy2pro.model.GameRun;
import me.daskabel.dummy2pro.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class GameRunRepositorySliceTest
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRunRepository gameRunRepository;

    @Test
    void findByUserUserIdOrderByStartedAtDesc_shouldReturnNewestRunFirst()
    {
        User user = userRepository.save(new User("runner", "hash"));

        GameRun olderRun = new GameRun(user, LocalDateTime.of(2026, 1, 1, 10, 0));
        olderRun.setDisplayName("alt");

        GameRun newerRun = new GameRun(user, LocalDateTime.of(2026, 1, 2, 10, 0));
        newerRun.setDisplayName("neu");

        gameRunRepository.save(olderRun);
        gameRunRepository.save(newerRun);

        List<GameRun> runs = gameRunRepository.findByUser_UserIdOrderByStartedAtDesc(user.getUserId());

        assertEquals(2, runs.size());
        assertEquals("neu", runs.get(0).getDisplayName());
        assertEquals("alt", runs.get(1).getDisplayName());
    }

    @Test
    void countByUserUserId_shouldCountOnlyRunsOfTheRequestedUser()
    {
        User firstUser = userRepository.save(new User("first-user", "hash"));
        User secondUser = userRepository.save(new User("second-user", "hash"));

        gameRunRepository.save(new GameRun(firstUser, LocalDateTime.now()));
        gameRunRepository.save(new GameRun(firstUser, LocalDateTime.now().plusMinutes(1)));
        gameRunRepository.save(new GameRun(secondUser, LocalDateTime.now()));

        assertEquals(2, gameRunRepository.countByUser_UserId(firstUser.getUserId()));
    }
}
