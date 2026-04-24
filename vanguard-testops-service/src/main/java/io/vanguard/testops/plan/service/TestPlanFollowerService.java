package io.vanguard.testops.plan.service;

import io.vanguard.testops.plan.domain.TestPlanFollower;
import io.vanguard.testops.plan.mapper.TestPlanFollowerMapper;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanFollowerService {

    @Resource
    private SqlSessionFactory sqlSessionFactory;

    public void batchSave(List<TestPlanFollower> testPlanFollowerList) {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        TestPlanFollowerMapper testPlanFollowerMapper = sqlSession.getMapper(TestPlanFollowerMapper.class);
        try {
            int insertIndex = 0;
            for (TestPlanFollower testPlanFollower : testPlanFollowerList) {
                testPlanFollowerMapper.insert(testPlanFollower);
                insertIndex++;
                if (insertIndex % 50 == 0) {
                    sqlSession.flushStatements();
                }
            }
            sqlSession.flushStatements();
        } finally {
            SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
        }
    }
}
