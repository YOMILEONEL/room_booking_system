package steve.bookingssystem.user.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    // Regression test: an account with customerType == null (a row predating that field, or
    // created outside the normal /api/register flow) but real firstName/lastName data used to
    // show its email forever in the UI, because getDisplayName() required customerType to
    // match before it would even look at the name fields.
    @Test
    void getDisplayName_usesNameEvenWhenCustomerTypeIsNull() {
        User user = new User();
        user.setEmail("steve@example.com");
        user.setCustomerType(null);
        user.setFirstName("Steve Leonel");
        user.setLastName("Yomi Mbiakop");

        assertThat(user.getDisplayName()).isEqualTo("Steve Leonel Yomi Mbiakop");
    }

    @Test
    void getDisplayName_usesOrganisationNameWhenSet() {
        User user = new User();
        user.setEmail("info@acme.example");
        user.setCustomerType(CustomerType.ORGANISATION);
        user.setOrganisationName("Acme GmbH");

        assertThat(user.getDisplayName()).isEqualTo("Acme GmbH");
    }

    @Test
    void getDisplayName_prefersFirstAndLastNameOverOrganisationName() {
        User user = new User();
        user.setEmail("both@example.com");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setOrganisationName("Some Org");

        assertThat(user.getDisplayName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void getDisplayName_fallsBackToEmailWhenNothingElseIsSet() {
        User user = new User();
        user.setEmail("nobody@example.com");

        assertThat(user.getDisplayName()).isEqualTo("nobody@example.com");
    }

    @Test
    void getDisplayName_fallsBackToEmailWhenOnlyFirstNameIsSet() {
        User user = new User();
        user.setEmail("partial@example.com");
        user.setFirstName("Ada");

        assertThat(user.getDisplayName()).isEqualTo("partial@example.com");
    }
}
