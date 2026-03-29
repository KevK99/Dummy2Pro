package me.daskabel.dummy2pro;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("db-smoke")
public class DBTestRunner implements CommandLineRunner
{
    private final JdbcTemplate jdbcTemplate;

    public DBTestRunner(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args)
    {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        System.out.println("DB Smoke Test: SELECT 1 -> " + one);
    }
}