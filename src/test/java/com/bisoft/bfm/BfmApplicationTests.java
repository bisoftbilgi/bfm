package com.bisoft.bfm;

import com.bisoft.bfm.dto.DatabaseStatus;
import com.bisoft.bfm.model.PostgresqlServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.*;


class BfmApplicationTests {

	private List<PostgresqlServer> pgList;
	private String pgUser = "postgres";

	private String pgPassword = "postgres";

	@BeforeEach
	public void prepare(){
		pgList = new ArrayList<>();
		PostgresqlServer pg1 = PostgresqlServer.builder().serverAddress("1.1.1.1").priority(1).databaseStatus(DatabaseStatus.MASTER).build();
		PostgresqlServer pg2 = PostgresqlServer.builder().serverAddress("1.1.1.2").priority(1).databaseStatus(DatabaseStatus.SLAVE).build();
		PostgresqlServer pg3 = PostgresqlServer.builder().serverAddress("1.1.1.3").priority(1).databaseStatus(DatabaseStatus.SLAVE).build();
		pgList.add(pg1);
		pgList.add(pg2);
		pgList.add(pg3);
	}

	@Test
	public void masterElectionTest(){

		pgList.stream().forEach(pg -> System.out.println(pg));

		assert pgList.size()==3;

	}

	@Test
	public void masterElectionTest2(){

		pgList.stream().forEach(pg -> System.out.println(pg));

		assert pgList.size()==3;

	}
}
