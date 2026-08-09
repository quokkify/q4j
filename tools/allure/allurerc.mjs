export default {
  name: "Q4J",
  output: "./allure-report",
  plugins: {
    awesome: {
      options: {
        reportName: "Q4J test report",
        singleFile: false,
        reportLanguage: "en",
        groupBy: ["epic", "feature", "story"],
      },
    },
  },
};
