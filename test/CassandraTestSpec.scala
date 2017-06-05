import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder._
import org.scalatest.{FunSpec, Matchers}
import services.utils.CassandraHelper

/**
  * Author: peter.marteen
  *
  */
class CassandraTestSpec extends FunSpec with Matchers {
  describe("Test cassandra connectivity") {
    it("Inserts and Retrievals from table") {


      val session = CassandraHelper.createCassandraSession().get

      session.execute("CREATE TABLE IF NOT EXISTS things (id int, name text, PRIMARY KEY (id))")
      session.execute("INSERT INTO things (id, name) VALUES (1, 'foo');")

      val selectStmt = select().column("name")
        .from("things")
        .where(QueryBuilder.eq("id", 1))
        .limit(1)

      val resultSet = session.execute(selectStmt)
      val row = resultSet.one()
      row.getString("name") should be("foo")
      session.execute("DROP TABLE things;")
    }
  }

}
