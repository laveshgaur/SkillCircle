package com.skillcircle.profile.config;

import com.skillcircle.profile.entity.Skill;
import com.skillcircle.profile.entity.SkillCategory;
import com.skillcircle.profile.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds the initial skill catalog with the top 200 technologies
 * across languages, frameworks, tools, databases, cloud, and devops.
 *
 * Only inserts skills that don't already exist (idempotent).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SkillSeederConfig {

    private final SkillRepository skillRepository;

    @Bean
    @Order(1)
    public CommandLineRunner seedSkills() {
        return args -> {
            Map<String, SkillCategory> skills = buildSkillCatalog();

            int inserted = 0;
            for (Map.Entry<String, SkillCategory> entry : skills.entrySet()) {
                if (!skillRepository.existsByNameIgnoreCase(entry.getKey())) {
                    skillRepository.save(Skill.builder()
                            .name(entry.getKey())
                            .category(entry.getValue())
                            .build());
                    inserted++;
                }
            }

            if (inserted > 0) {
                log.info("Seeded {} new skills into the catalog (total catalog: {})",
                        inserted, skillRepository.count());
            } else {
                log.info("Skill catalog already populated ({} skills)", skillRepository.count());
            }
        };
    }

    private Map<String, SkillCategory> buildSkillCatalog() {
        Map<String, SkillCategory> skills = new LinkedHashMap<>();

        // ==================== LANGUAGES (30) ====================
        for (String s : new String[]{
                "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust",
                "C", "C++", "C#", "Kotlin", "Swift", "Ruby", "PHP",
                "Scala", "Dart", "Elixir", "Haskell", "Clojure", "R",
                "Lua", "Perl", "Shell", "Bash", "SQL", "GraphQL",
                "HTML", "CSS", "Solidity", "MATLAB", "Julia"
        }) { skills.put(s, SkillCategory.LANGUAGE); }

        // ==================== FRAMEWORKS (55) ====================
        for (String s : new String[]{
                "Spring Boot", "Spring Cloud", "Hibernate", "Quarkus", "Micronaut",
                "React", "Next.js", "Vue.js", "Nuxt.js", "Angular", "Svelte",
                "SvelteKit", "Astro", "Remix", "Gatsby",
                "Express.js", "NestJS", "Fastify", "Koa",
                "Django", "Flask", "FastAPI", "Tornado",
                "Ruby on Rails", "Sinatra",
                "ASP.NET", "Blazor", ".NET MAUI",
                "Gin", "Echo", "Fiber",
                "Actix", "Axum", "Rocket",
                "Flutter", "React Native", "Ionic",
                "Electron", "Tauri",
                "TailwindCSS", "Bootstrap", "Material UI", "Chakra UI",
                "Three.js", "D3.js", "Chart.js",
                "Spring Security", "Passport.js",
                "Prisma", "Sequelize", "TypeORM", "Drizzle ORM",
                "tRPC", "Hono", "Ktor", "Phoenix"
        }) { skills.put(s, SkillCategory.FRAMEWORK); }

        // ==================== TOOLS (45) ====================
        for (String s : new String[]{
                "Git", "GitHub", "GitLab", "Bitbucket",
                "Docker", "Kubernetes", "Helm", "Podman",
                "Webpack", "Vite", "esbuild", "Turbopack", "Rollup",
                "npm", "Yarn", "pnpm", "Maven", "Gradle",
                "VS Code", "IntelliJ IDEA", "Vim", "Neovim",
                "Postman", "Insomnia", "Swagger", "OpenAPI",
                "Jest", "Vitest", "Cypress", "Playwright", "JUnit", "Selenium",
                "ESLint", "Prettier", "SonarQube",
                "Figma", "Storybook",
                "Jira", "Linear", "Notion",
                "Nginx", "Apache", "Caddy",
                "RabbitMQ", "Apache Kafka", "NATS"
        }) { skills.put(s, SkillCategory.TOOL); }

        // ==================== DATABASES (20) ====================
        for (String s : new String[]{
                "PostgreSQL", "MySQL", "MariaDB", "SQLite",
                "MongoDB", "DynamoDB", "CouchDB", "Cassandra",
                "Redis", "Memcached",
                "Elasticsearch", "OpenSearch",
                "Neo4j", "ArangoDB",
                "Supabase", "Firebase", "PlanetScale",
                "ClickHouse", "TimescaleDB", "CockroachDB"
        }) { skills.put(s, SkillCategory.DATABASE); }

        // ==================== CLOUD (25) ====================
        for (String s : new String[]{
                "AWS", "Amazon S3", "Amazon EC2", "AWS Lambda", "Amazon ECS",
                "Google Cloud Platform", "Cloud Run", "BigQuery",
                "Microsoft Azure", "Azure Functions",
                "Vercel", "Netlify", "Railway", "Render", "Fly.io",
                "Cloudflare", "Cloudflare Workers",
                "DigitalOcean", "Linode", "Hetzner",
                "Heroku", "Supabase Cloud",
                "Terraform", "Pulumi", "AWS CDK"
        }) { skills.put(s, SkillCategory.CLOUD); }

        // ==================== DEVOPS (20) ====================
        for (String s : new String[]{
                "GitHub Actions", "GitLab CI/CD", "Jenkins", "CircleCI",
                "ArgoCD", "Flux",
                "Prometheus", "Grafana", "Datadog", "New Relic",
                "ELK Stack", "Loki", "Jaeger",
                "Ansible", "Chef", "Puppet",
                "Vagrant", "Packer",
                "HashiCorp Vault", "Consul"
        }) { skills.put(s, SkillCategory.DEVOPS); }

        // ==================== DOMAINS (15) ====================
        for (String s : new String[]{
                "Machine Learning", "Deep Learning", "NLP", "Computer Vision",
                "Data Science", "Data Engineering",
                "Blockchain", "Web3", "Smart Contracts",
                "IoT", "Embedded Systems",
                "Game Development", "AR/VR",
                "Cybersecurity", "DevSecOps"
        }) { skills.put(s, SkillCategory.DOMAIN); }

        return skills;
    }
}
