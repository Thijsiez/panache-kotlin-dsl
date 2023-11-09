INSERT INTO DEPARTMENT (ID, NAME) VALUES
(1, 'Sales'),
(2, 'HR'),
(3, 'Engineering'),
(4, 'Management');

INSERT INTO EMPLOYEE (ID, EMPLOYEE_NO, FIRST_NAME, LAST_NAME, GENDER, BIRTH_DATE, DEPARTMENT_ID, SALARY) VALUES
(1, 1, 'Steve', 'Kainzow', 'M', '1950-08-11', 3, 2600),
(2, 4, 'John', 'Doenawho', 'M', '1960-04-30', 4, 1500000),
(3, 8, 'Mary', 'Smith', 'F', '1986-02-14', 2, 80000),
(4, 15, 'Barbara', 'Jackson', 'F', '2002-11-30', 1, 50000),
(5, 16, 'Riley', 'Anderson', 'X', '1971-01-29', 3, 250000),
(6, 23, 'Susan', 'Ikcicjow', 'F', '1986-07-05', 4, 4500000),
(7, 42, 'Matthew', 'Heads', 'M', '1994-10-13', 3, 55000);

INSERT INTO CLIENT (ID, NAME) VALUES
(1, 'Acme Corporation'),
(2, 'Videos, LLC');

INSERT INTO ASSIGNMENT (ID, EMPLOYEE_ID, CLIENT_ID, ROLE) VALUES
(1, 6, 2, 'Former CEO'),
(2, 4, 1, 'Sales Rep'),
(3, 7, 1, 'Engineer');
