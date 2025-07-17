import com.redmath.bookmanagement.BookApplication;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(classes= BookApplication.class)
@AutoConfigureMockMvc
public class BookApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetBookByIdSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/books/123"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(123)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("Clean Code")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.author", Matchers.is("Robert C. Martin")));
    }

    @Test
    public void testGetBookByIdNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/books/999"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testGetAllBooksSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/books"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].title", Matchers.is("Clean Code")));
    }

    @Test
    public void testCreateBookSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Domain-Driven Design",
                                    "author": "Eric Evans",
                                    "isbn": "9780321125217",
                                    "publishedYear": 2003
                                }
                                """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk()) // you return 200 in controller
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.notNullValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("Domain-Driven Design")));
    }

    @Test
    public void testUpdateBookSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/books/124")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Refactoring (2nd Edition)",
                                    "author": "Martin Fowler",
                                    "isbn": "9780201485677",
                                    "publishedYear": 2018
                                }
                                """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("Refactoring (2nd Edition)")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.publishedYear", Matchers.is(2018)));
    }

    @Test
    public void testDeleteBookSuccess() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/books/124")).andDo(MockMvcResultHandlers.print()).andExpect(MockMvcResultMatchers.status().isNoContent());
    }
}
