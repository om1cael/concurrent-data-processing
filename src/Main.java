import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        List<Integer> numberList = new ArrayList<>();
        for(int i = 0; i<=5_000_0; i++) numberList.add(i);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        SumCalculation sumCalculation = new SumCalculation(numberList);
        int sum = forkJoinPool.invoke(sumCalculation);

        Future<Integer> averageResult = executorService.submit(new AverageCalculator(numberList, sum));
        int average = averageResult.get();

        List<Integer> minMax = forkJoinPool.invoke(new FindMinMax(numberList));

        System.out.println("Sum: " + sum);
        System.out.println("Average: " + average);
        System.out.println("Min: " + minMax.getFirst());
        System.out.println("Max: " + minMax.getLast());


        executorService.close();
        forkJoinPool.close();
    }
}

class SumCalculation extends RecursiveTask<Integer> {
    List<Integer> numberList;

    public SumCalculation(List<Integer> numberList) {
        this.numberList = numberList;
    }

    @Override
    protected Integer compute() {
        if(this.numberList.size() <= 1000) {
            return this.numberList.stream().mapToInt(Integer::intValue).sum();
        }

        int mid = this.numberList.size() / 2;
        SumCalculation leftTask = new SumCalculation(this.numberList.subList(0, mid));
        SumCalculation rightTask = new SumCalculation(this.numberList.subList(mid, this.numberList.size()));

        leftTask.fork();
        rightTask.fork();

        return leftTask.join() + rightTask.join();
    }
}

class AverageCalculator implements Callable<Integer> {
    List<Integer> numList;
    int sum;

    public AverageCalculator(List<Integer> numList, int sum) {
        this.numList = numList;
        this.sum = sum;
    }

    @Override
    public Integer call() {
        return sum / numList.size();
    }
}

class FindMinMax extends RecursiveTask<List<Integer>> {
    List<Integer> numberList;

    public FindMinMax(List<Integer> numberList) {
        this.numberList = numberList;
    }

    @Override
    protected List<Integer> compute() {
        if(this.numberList.size() <= 1000) {
            return this.numberList.stream()
                    .collect(Collectors.teeing(
                            Collectors.minBy(Integer::compareTo),
                            Collectors.maxBy(Integer::compareTo),
                            (min, max) -> List.of(min.get(), max.get())
                    ));
        }

        int mid = this.numberList.size() / 2;
        FindMinMax leftArrayItems = new FindMinMax(this.numberList.subList(0, mid));
        FindMinMax rightArrayItems = new FindMinMax(this.numberList.subList(mid, this.numberList.size()));

        leftArrayItems.fork();
        rightArrayItems.fork();

        AtomicInteger min = new AtomicInteger(0);
        AtomicInteger max = new AtomicInteger(0);

        List<Integer> leftMinMax = leftArrayItems.join();
        List<Integer> rightMinMax = rightArrayItems.join();

        min.set(Math.min(leftMinMax.getFirst(), rightMinMax.getFirst()));
        max.set(Math.max(rightMinMax.getLast(), rightMinMax.getLast()));

        return List.of(min.get(), max.get());
    }
}