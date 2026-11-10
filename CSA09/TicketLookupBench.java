import java.util.*;

public class TicketLookupBench {
    public static void main(String[] args) {
        int[] sizes = {20, 150, 500, 5000, 50000};
        for (int n : sizes) {
            String[] arr = new String[n];
            ArrayList<String> list = new ArrayList<>();
            HashMap<String, Integer> map = new HashMap<>();
            for (int i = 0; i < n; i++) {
                arr[i] = "TN" + i;
                list.add(arr[i]);
                map.put(arr[i], i);
            }
            String key = "TN" + (n - 1);          // worst case for linear scan
            int reps = 2000;
            long t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) {
                for (int i = 0; i < n; i++) if (arr[i].equals(key)) break;
            }
            long tArr = (System.nanoTime() - t0) / reps;
            t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) list.indexOf(key);
            long tList = (System.nanoTime() - t0) / reps;
            t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) map.get(key);
            long tMap = (System.nanoTime() - t0) / reps;
            System.out.printf("n=%6d  array=%8d ns  ArrayList=%8d ns  HashMap=%6d ns%n",
                    n, tArr, tList, tMap);
        }
    }
}
